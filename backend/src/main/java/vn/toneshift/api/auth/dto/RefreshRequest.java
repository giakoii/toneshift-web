package vn.toneshift.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Carries a refresh token, for both rotation and logout.
 *
 * @param refreshToken the raw refresh token previously issued
 */
public record RefreshRequest(@NotBlank String refreshToken) {
}
