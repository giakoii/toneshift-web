package vn.toneshift.api.common.exception;

/**
 * Exception for resources that do not exist or are not visible to the caller (HTTP 404).
 */
public class NotFoundException extends ApiException {

    /**
     * Creates an exception using the default message of the given error code.
     *
     * @param code the error code
     */
    public NotFoundException(ErrorCode code) {
        super(code);
    }

    /**
     * Creates an exception with the given error code and a custom message.
     *
     * @param code    the error code
     * @param message the custom client-facing message
     */
    public NotFoundException(ErrorCode code, String message) {
        super(code, message);
    }
}
