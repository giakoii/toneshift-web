package vn.toneshift.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration with email and password.
 *
 * @param name     the display name
 * @param email    the email, stored lowercase and unique case-insensitively
 * @param password the raw password; capped at 72 characters because BCrypt ignores anything beyond
 */
public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "phải có cả chữ và số")
        String password) {
}
