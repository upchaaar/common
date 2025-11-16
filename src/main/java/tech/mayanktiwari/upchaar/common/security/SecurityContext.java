package tech.mayanktiwari.upchaar.common.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecurityContext {
    private static final ThreadLocal<UserContext> USER_CONTEXT = new ThreadLocal<>();

    private SecurityContext() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /** Set the user context for the current thread */
    public static void setUserContext(String userId, String role) {
        if (userId == null || userId.isBlank()) {
            log.warn("Attempted to set null or blank user ID");
            return;
        }
        USER_CONTEXT.set(new UserContext(userId, role));
        log.debug("User context set: userId={}, role={}", userId, role);
    }

    public static String getUserId() {
        UserContext context = USER_CONTEXT.get();
        return context != null ? context.getUserId() : null;
    }

    /** Get the current user role */
    public static String getRole() {
        UserContext context = USER_CONTEXT.get();
        return context != null ? context.getRole() : null;
    }

    /** Check if user context is set */
    public static boolean isSet() {
        return USER_CONTEXT.get() != null;
    }

    /** Clear the user context (important for thread pool reuse) */
    public static void clear() {
        USER_CONTEXT.remove();
        log.debug("User context cleared");
    }

    /** Validate that user context is set, throw exception if not */
    public static void requireUser() {
        if (!isSet()) {
            throw new IllegalStateException("User context is not set");
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class UserContext {
        private String userId;
        private String role;
    }
}
