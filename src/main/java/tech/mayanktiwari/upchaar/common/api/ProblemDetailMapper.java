package tech.mayanktiwari.upchaar.common.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

public final class ProblemDetailMapper {

    private ProblemDetailMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static ProblemDetail of(
            HttpStatus status, String title, String detail, String instance, String correlationId
    ) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create("about:blank"));
        if (instance != null)
            pd.setInstance(URI.create(instance));
        pd.setProperty("timestamp", Instant.now()
                                           .toString());
        if (correlationId != null)
            pd.setProperty("correlationId", correlationId);
        return pd;
    }

    public static ProblemDetail validationError(
            Map<String, String> fieldErrors, String instance, String correlationId
    ) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setTitle("Validation Error");
        pd.setType(URI.create("about:blank"));
        if (instance != null)
            pd.setInstance(URI.create(instance));
        pd.setProperty("timestamp", Instant.now()
                                           .toString());
        pd.setProperty("errors", fieldErrors);
        if (correlationId != null)
            pd.setProperty("correlationId", correlationId);
        return pd;
    }

    public static ProblemDetail accessDenied(String detail, String instance, String correlationId) {
        return of(HttpStatus.FORBIDDEN, "Access Denied", detail, instance, correlationId);
    }

    public static ProblemDetail notFound(String detail, String instance, String correlationId) {
        return of(HttpStatus.NOT_FOUND, "Resource Not Found", detail, instance, correlationId);
    }

    public static ProblemDetail conflict(String detail, String instance, String correlationId) {
        return of(HttpStatus.CONFLICT, "Conflict", detail, instance, correlationId);
    }

    public static ProblemDetail internalError(String detail, String instance, String correlationId) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", detail, instance, correlationId);
    }
}