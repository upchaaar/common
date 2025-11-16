package tech.mayanktiwari.upchaar.common.pii;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PiiMasker {
    private PiiMasker() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /** Mask phone: +1234567890 -> ****7890 */
    public static String maskPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            log.warn("Attempted to mask null or blank phone number");
            return "***";
        }
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");
        if (cleaned.length() <= 4) {
            return "***";
        }
        String maskedPart = "*".repeat(cleaned.length() - 4);
        String visiblePart = cleaned.substring(cleaned.length() - 4);
        return maskedPart + visiblePart;
    }

    /** Mask name: John Doe -> J*** D*** */
    public static String maskName(String name) {
        if (name == null || name.isBlank()) {
            log.warn("Attempted to mask null or blank name");
            return "***";
        }

        String[] parts = name.trim()
                             .split("\\s+");
        StringBuilder masked = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.length() > 0) {
                masked.append(part.charAt(0))
                      .append("***");
                if (i < parts.length - 1) {
                    masked.append(" ");
                }
            }
        }

        return masked.toString();
    }

    /** Mask email: example@domain.com -> e***@d***.com */
    public String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            log.warn("Attempted to mask null or blank email");
            return email;
        }
        String[] parts = email.split("@");
        if (parts.length != 2) {
            log.warn("Invalid email format: {}", email);
            return "***";
        }

        String localPart = parts[0];
        String domainPart = parts[1];

        String maskedLocalPart = localPart.length() > 1 ? localPart.charAt(0) + "***" : "***";
        String maskedDomainPart = domainPart.length() > 1 ? domainPart.charAt(0) + "***" : "***";

        return maskedLocalPart + "@" + maskedDomainPart;
    }

    /** Generic masking: show first and last char, mask middle */
    public static String maskGeneric(String value) {
        if (value == null || value.isBlank()) {
            log.warn("Attempted to mask null or blank value");
            return "***";
        }

        if (value.length() <= 2) {
            return "***";
        }

        return value.charAt(0) + "***" + value.substring(1);
    }
}
