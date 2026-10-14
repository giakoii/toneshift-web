package vn.toneshift.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Sign-in with email and password.
 *
 * @param email    the email to sign in with
 * @param password the raw password
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password) {
}
