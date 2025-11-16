package tech.mayanktiwari.upchaar.common.idempotency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.DigestUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tech.mayanktiwari.upchaar.common.security.SecurityContext;
import tech.mayanktiwari.upchaar.common.security.TenantContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyStore store;
    private final IdempotencyProperties properties;

    /*
      IdempotencyStore (expected minimal contract used by this filter)
      ---------------------------------------------------------------
      Optional<IdempotencyRecord> findByKey(String tenantId, String idempotencyKey);
      boolean createPlaceholderIfAbsent(IdempotencyRecord record); // atomic create-if-absent, returns true if created by caller
      void updateResponse(String tenantId, String idempotencyKey, int responseStatus, String responseBody, String resourceLocation);
     */

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String method = request.getMethod();
        return !(HttpMethod.POST.matches(method) || HttpMethod.PUT.matches(method) || HttpMethod.PATCH.matches(method));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain
    ) throws ServletException, IOException {

        // wrap request/response so we can read body/status multiple times
        ContentCachingRequestWrapper cachingRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper(response);

        final String tenantId = TenantContext.getTenantId();
        final String userId = SecurityContext.getUserId();
        final String idempotencyKey = request.getHeader(properties.getHeaderName());

        // if no key or missing tenant -> don't enforce idempotency, continue normal flow
        if (idempotencyKey == null || idempotencyKey.isBlank() || tenantId == null) {
            chain.doFilter(cachingRequest, response);
            return;
        }

        // compute canonical string and MD5 hash (request identity)
        String requestBody = getRequestBodyAsString(cachingRequest);
        String canonical = request.getMethod() + "|" + request.getRequestURI() + "|" + tenantId + "|" + userId + "|" + requestBody;
        String requestHash = DigestUtils.md5DigestAsHex(canonical.getBytes(StandardCharsets.UTF_8));

        // Try to atomically create a placeholder record.
        // If returns true -> this thread is the owner and should execute request and then update response.
        IdempotencyStore.IdempotencyRecord placeholder = IdempotencyStore.IdempotencyRecord.builder()
                                                                                           .tenantId(tenantId)
                                                                                           .userId(userId)
                                                                                           .idempotencyKey(idempotencyKey)
                                                                                           .requestHash(requestHash)
                                                                                           .createdAt(Instant.now())
                                                                                           .expiresAt(Instant.now()
                                                                                                             .plus(properties.getTtl()))
                                                                                           .build();

        boolean created = store.createPlaceholderIfAbsent(placeholder);

        if (created) {
            // this thread will process the request and then update the record with the response
            try {
                chain.doFilter(cachingRequest, cachingResponse);

                int status = cachingResponse.getStatus();
                String responseBody = properties.isStoreFullResponse() ?
                        getResponseBodyAsString(cachingResponse) :
                        null;
                String location = cachingResponse.getHeader("Location");

                // persist response so future retries can be short-circuited
                store.updateResponse(tenantId, idempotencyKey, status, responseBody, location);

                // copy response body to actual response output stream
                cachingResponse.copyBodyToResponse();

            } catch (Exception ex) {
                // on failure, you might want to remove placeholder or mark failed.
                // For simplicity, remove label so future attempts can try again (implementation choice).
                log.error("Error while processing idempotent request for key {}", idempotencyKey, ex);
                // Optionally: store a failure marker. Here we delete placeholder so a retry can try again.
                store.deleteIfPlaceholder(tenantId, idempotencyKey, requestHash);
                throw ex;
            }
            return;
        } else {
            // placeholder already exists => someone else is/was processing a request with same idempotency key
            Optional<IdempotencyStore.IdempotencyRecord> existingOpt = store.findByKey(tenantId, idempotencyKey);
            if (existingOpt.isEmpty()) {
                // race: placeholder creation failed but record is gone — try again (simple fallback)
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                response.getWriter()
                        .write("{\"title\":\"Idempotency error\",\"detail\":\"Please retry\"}");
                return;
            }

            IdempotencyStore.IdempotencyRecord existing = existingOpt.get();

            // 1) request hash mismatch -> 409 Conflict
            if (!requestHash.equals(existing.getRequestHash())) {
                response.setStatus(HttpStatus.CONFLICT.value());
                response.setContentType("application/json");
                String body = """
                              {"type":"about:blank",
                              "title":"Idempotency Conflict",
                              "status":409,
                              "detail":"Request body does not match previous request for same Idempotency-Key",
                              "instance":"%s"}
                              """.formatted(request.getRequestURI());
                response.getWriter()
                        .write(body);
                return;
            }

            // 2) if response already stored -> short-circuit and return stored response
            if (existing.getResponseStatus() != null) {
                response.setStatus(existing.getResponseStatus());
                if (existing.getResourceLocation() != null) {
                    response.setHeader("Location", existing.getResourceLocation());
                }
                if (properties.isStoreFullResponse() && existing.getResponseBody() != null) {
                    response.setContentType("application/json");
                    response.getWriter()
                            .write(existing.getResponseBody());
                }
                return;
            }

            // 3) placeholder exists but response not yet stored -> in-progress. We choose to return 409 (locked)
            response.setStatus(HttpStatus.CONFLICT.value());
            response.setContentType("application/json");
            String body = """
                          {"type":"about:blank",
                          "title":"Idempotency In-Progress",
                          "status":409,
                          "detail":"A request with same Idempotency-Key is currently being processed. Please retry later.",
                          "instance":"%s"}
                          """.formatted(request.getRequestURI());
            response.getWriter()
                    .write(body);
            return;
        }
    }

    private static String getRequestBodyAsString(ContentCachingRequestWrapper request) throws IOException {
        // trigger caching of the request body (ContentCachingRequestWrapper caches on first read)
        byte[] buf = request.getContentAsByteArray();
        if (buf == null || buf.length == 0) {
            return "";
        }
        return new String(buf, (request.getCharacterEncoding() == null ?
                StandardCharsets.UTF_8 :
                request.getCharacterEncoding()).toString());
    }

    private static String getResponseBodyAsString(ContentCachingResponseWrapper response) throws IOException {
        byte[] buf = response.getContentAsByteArray();
        if (buf == null || buf.length == 0) {
            return "";
        }
        return new String(buf, (response.getCharacterEncoding() == null ?
                StandardCharsets.UTF_8 :
                response.getCharacterEncoding()).toString());
    }
}