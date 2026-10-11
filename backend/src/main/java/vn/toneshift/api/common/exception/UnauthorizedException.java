package vn.toneshift.api.common.exception;

/**
 * Exception for missing or invalid authentication credentials (HTTP 401).
 */
public class UnauthorizedException extends ApiException {

    /**
     * Creates an exception with the generic {@link ErrorCode#UNAUTHORIZED} code.
     */
    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }

    /**
     * Creates an exception using the default message of the given error code.
     *
     * @param code the error code
     */
    public UnauthorizedException(ErrorCode code) {
        super(code);
    }

    /**
     * Creates an exception with the given error code and a custom message.
     *
     * @param code    the error code
     * @param message the custom client-facing message
     */
    public UnauthorizedException(ErrorCode code, String message) {
        super(code, message);
    }
}
