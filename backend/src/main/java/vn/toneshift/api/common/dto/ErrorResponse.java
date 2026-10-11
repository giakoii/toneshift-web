package vn.toneshift.api.common.dto;

import vn.toneshift.api.common.exception.ErrorCode;

import java.time.Instant;
import java.util.List;

/**
 * Uniform JSON body returned for every API error.
 *
 * @param timestamp time the error occurred
 * @param status    HTTP status code
 * @param code      application error code name
 * @param message   client-facing error message
 * @param path      request path that caused the error
 * @param traceId   trace id used to correlate with server logs
 * @param errors    field-level errors, empty unless validation failed
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String traceId,
        List<FieldErrorItem> errors
) {

    /**
     * Builds an error response for the given code without field errors.
     *
     * @param code    the error code
     * @param message the client-facing message
     * @param path    the request path
     * @param traceId the request trace id
     * @return the error response
     */
    public static ErrorResponse of(ErrorCode code, String message, String path, String traceId) {
        return new ErrorResponse(Instant.now(), code.status().value(), code.name(), message, path, traceId, List.of());
    }

    /**
     * Builds a validation error response carrying the per-field errors.
     *
     * @param message the client-facing message
     * @param path    the request path
     * @param traceId the request trace id
     * @param errors  the field-level errors
     * @return the validation error response
     */
    public static ErrorResponse ofValidation(String message, String path, String traceId, List<FieldErrorItem> errors) {
        ErrorCode code = ErrorCode.VALIDATION_FAILED;
        return new ErrorResponse(Instant.now(), code.status().value(), code.name(), message, path, traceId, errors);
    }
}
