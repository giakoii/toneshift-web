package vn.toneshift.api.common.exception;

/**
 * Exception for requests that conflict with the current state of a resource (HTTP 409).
 */
public class ConflictException extends ApiException {

    /**
     * Creates an exception using the default message of the given error code.
     *
     * @param code the error code
     */
    public ConflictException(ErrorCode code) {
        super(code);
    }

    /**
     * Creates an exception with the given error code and a custom message.
     *
     * @param code    the error code
     * @param message the custom client-facing message
     */
    public ConflictException(ErrorCode code, String message) {
        super(code, message);
    }
}
