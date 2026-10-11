package vn.toneshift.api.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import vn.toneshift.api.common.dto.ErrorResponse;
import vn.toneshift.api.common.dto.FieldErrorItem;

import java.util.List;

/**
 * Translates exceptions thrown by controllers into a uniform {@link ErrorResponse}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles request body validation failures and returns the per-field errors (HTTP 400).
     *
     * @param ex      the validation exception
     * @param request the current HTTP request
     * @return the error response with the field errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorItem)
                .toList();
        return respond(ErrorResponse.ofValidation(ErrorCode.VALIDATION_FAILED.defaultMessage(), request.getRequestURI(), traceId(), errors));
    }

    /**
     * Handles constraint violations on request parameters and returns the per-field errors (HTTP 400).
     *
     * @param ex      the constraint violation exception
     * @param request the current HTTP request
     * @return the error response with the field errors
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex, HttpServletRequest request) {
        List<FieldErrorItem> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorItem(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return respond(ErrorResponse.ofValidation(ErrorCode.VALIDATION_FAILED.defaultMessage(), request.getRequestURI(), traceId(), errors));
    }

    /**
     * Handles unreadable JSON, type mismatches and missing parameters (HTTP 400).
     *
     * @param ex      the exception describing the malformed request
     * @param request the current HTTP request
     * @return the MALFORMED_REQUEST error response
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception ex, HttpServletRequest request) {
        return respond(ErrorResponse.of(ErrorCode.MALFORMED_REQUEST, ErrorCode.MALFORMED_REQUEST.defaultMessage(), request.getRequestURI(), traceId()));
    }

    /**
     * Handles business exceptions using the status and code they carry.
     *
     * @param ex      the business exception
     * @param request the current HTTP request
     * @return the error response built from the exception's error code
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        return respond(ErrorResponse.of(ex.code(), ex.getMessage(), request.getRequestURI(), traceId()));
    }

    /**
     * Handles optimistic locking failures caused by concurrent edits (HTTP 409).
     *
     * @param ex      the optimistic locking exception
     * @param request the current HTTP request
     * @return the VERSION_CONFLICT error response
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        return respond(ErrorResponse.of(ErrorCode.VERSION_CONFLICT, ErrorCode.VERSION_CONFLICT.defaultMessage(), request.getRequestURI(), traceId()));
    }

    /**
     * Handles database constraint violations without leaking SQL details (HTTP 409).
     *
     * @param ex      the data integrity violation exception
     * @param request the current HTTP request
     * @return the error response mapped from the violated constraint
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        ErrorCode code = mapConstraintToErrorCode(ex);
        return respond(ErrorResponse.of(code, code.defaultMessage(), request.getRequestURI(), traceId()));
    }

    /**
     * Fallback handler that logs unexpected errors and returns a generic message (HTTP 500).
     *
     * @param ex      the unexpected exception
     * @param request the current HTTP request
     * @return the INTERNAL_ERROR error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        String traceId = traceId();
        log.error("Unexpected error, traceId={}", traceId, ex);
        return respond(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), request.getRequestURI(), traceId));
    }

    // Wraps the error body in a response entity using the status stored in the body.
    private ResponseEntity<ErrorResponse> respond(ErrorResponse body) {
        return ResponseEntity.status(HttpStatus.valueOf(body.status())).body(body);
    }

    // Converts a Spring field error into a response field error item.
    private FieldErrorItem toFieldErrorItem(FieldError error) {
        return new FieldErrorItem(error.getField(), error.getDefaultMessage());
    }

    // Returns the current request trace id from the logging context, or "unknown" if absent.
    private String traceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : "unknown";
    }

    // Maps a violated database constraint name to a specific error code, defaulting to CONFLICT.
    private ErrorCode mapConstraintToErrorCode(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        if (message == null) {
            return ErrorCode.CONFLICT;
        }
        if (message.contains("uq_users_email")) {
            return ErrorCode.EMAIL_ALREADY_EXISTS;
        }
        if (message.contains("uq_report_once")) {
            return ErrorCode.ALREADY_REPORTED;
        }
        return ErrorCode.CONFLICT;
    }
}
