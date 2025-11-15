package tech.mayanktiwari.upchaar.common.security;

public final class RoleConstants {
    private RoleConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Roles Names
    public static final String HOSPITAL_ADMIN = "HOSPITAL_ADMIN";
    public static final String DOCTOR = "DOCTOR";
    public static final String PATIENT = "PATIENT";

    // Roles prefixes for Spring Security
    public static final String ROLE_HOSPITAL_ADMIN = "ROLE_" + HOSPITAL_ADMIN;
    public static final String ROLE_DOCTOR = "ROLE_" + DOCTOR;
    public static final String ROLE_PATIENT = "ROLE_" + PATIENT;
}
