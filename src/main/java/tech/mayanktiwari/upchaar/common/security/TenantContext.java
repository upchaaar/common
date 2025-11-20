package tech.mayanktiwari.upchaar.common.security;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TenantContext {
    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /** Check if tenant context is set */
    public static boolean isSet() {
        return TENANT_ID.get() != null;
    }

    /** Get the tenant ID for the current thread */
    public static String getTenantId() {
        return TENANT_ID.get();
    }

    /** Set the tenant ID for the current thread */
    public static void setTenantId(String tenantId) {
        if (Objects.isNull(tenantId) || tenantId.isBlank()) {
            log.warn("Attempted to set null or blank tenant ID");
            return;
        } else {
            TENANT_ID.set(tenantId);
            log.debug("Set tenant ID to: {}", tenantId);
        }
    }

    /** Clear the tenant context (important for thread pool reuse) */
    public static void clear() {
        TENANT_ID.remove();
        log.debug("Tenant context cleared");
    }

    /** Validate that tenant context is set, throw exception if not */
    public static void requireTenant() {
        if (!isSet()) {
            throw new IllegalStateException("Tenant context is not set");
        }
    }
}
