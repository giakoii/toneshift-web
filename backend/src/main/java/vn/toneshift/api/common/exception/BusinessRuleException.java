package vn.toneshift.api.common.exception;

/**
 * Exception for well-formed requests that violate a business rule (HTTP 422).
 */
public class BusinessRuleException extends ApiException {

    /**
     * Creates an exception using the default message of the given error code.
     *
     * @param code the error code
     */
    public BusinessRuleException(ErrorCode code) {
        super(code);
    }

    /**
     * Creates an exception with the given error code and a custom message.
     *
     * @param code    the error code
     * @param message the custom client-facing message
     */
    public BusinessRuleException(ErrorCode code, String message) {
        super(code, message);
    }
}
