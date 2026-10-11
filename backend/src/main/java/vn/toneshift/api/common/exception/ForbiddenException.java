package vn.toneshift.api.common.exception;

/**
 * Exception for authenticated callers lacking permission for an action (HTTP 403).
 */
public class ForbiddenException extends ApiException {

    /**
     * Creates an exception with the generic {@link ErrorCode#FORBIDDEN} code.
     */
    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }

    /**
     * Creates an exception using the default message of the given error code.
     *
     * @param code the error code
     */
    public ForbiddenException(ErrorCode code) {
        super(code);
    }

    /**
     * Creates an exception with the given error code and a custom message.
     *
     * @param code    the error code
     * @param message the custom client-facing message
     */
    public ForbiddenException(ErrorCode code, String message) {
        super(code, message);
    }
}
