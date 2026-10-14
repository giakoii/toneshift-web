package vn.toneshift.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Changes to the signed-in user's own profile.
 *
 * @param name  the new display name
 * @param image the new avatar URL, or {@code null} to clear it; must be https so the frontend
 *              never loads mixed content
 */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) @Pattern(regexp = "^https://.+", message = "phải là URL https") String image) {
}
