package vn.toneshift.api.common.logging;

/**
 * Masks values that must never appear in logs in full, such as emails, tokens and passwords.
 */
public final class LogMasker {

    private static final String MASK = "***";
    private static final int TOKEN_VISIBLE_SUFFIX = 4;

    // Utility class, not instantiable.
    private LogMasker() {
    }

    /**
     * Masks the local part of an email, keeping only its first character and the domain.
     *
     * @param email the email to mask
     * @return the masked email, or {@code ***} if it is blank or has no domain
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return MASK;
        }
        int at = email.indexOf('@');
        if (at < 1) {
            return MASK;
        }
        return email.charAt(0) + MASK + email.substring(at);
    }

    /**
     * Masks a token, keeping only its last four characters so log lines can still be correlated.
     *
     * @param token the token to mask
     * @return the masked token
     */
    public static String maskToken(String token) {
        if (token == null || token.length() <= TOKEN_VISIBLE_SUFFIX) {
            return MASK;
        }
        return MASK + token.substring(token.length() - TOKEN_VISIBLE_SUFFIX);
    }
}
