package vn.toneshift.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Sets or changes the signed-in user's password.
 *
 * @param currentPassword the password in use; intentionally optional, because an account created
 *                        through a provider has none yet and is setting its first one
 * @param newPassword     the new password
 */
public record ChangePasswordRequest(
        String currentPassword,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "phải có cả chữ và số")
        String newPassword) {
}
