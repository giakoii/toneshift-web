package vn.toneshift.api.common.dto;

/**
 * A single field-level validation error.
 *
 * @param field   name of the invalid field
 * @param message description of the validation failure
 */
public record FieldErrorItem(String field, String message) {
}
