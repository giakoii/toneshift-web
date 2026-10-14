package vn.toneshift.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Requests a password reset link.
 *
 * @param email the email to send the link to
 */
public record ForgotPasswordRequest(@NotBlank @Email String email) {
}
