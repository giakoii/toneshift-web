package vn.toneshift.api.common.exception;

/**
 * Base runtime exception for business errors that map to an {@link ErrorCode}.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;

    /**
     * Creates an exception using the default message of the given error code.
     *
     * @param code the error code
     */
    public ApiException(ErrorCode code) {
        this(code, code.defaultMessage());
    }

    /**
     * Creates an exception with the given error code and a custom message.
     *
     * @param code    the error code
     * @param message the custom client-facing message
     */
    public ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Returns the error code carried by this exception.
     *
     * @return the error code
     */
    public ErrorCode code() {
        return code;
    }
}
