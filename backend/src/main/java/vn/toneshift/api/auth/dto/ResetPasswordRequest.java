package vn.toneshift.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Sets a new password using a reset token.
 *
 * @param token       the raw token from the reset link
 * @param newPassword the new password, under the same rules as registration
 */
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "phải có cả chữ và số")
        String newPassword) {
}
