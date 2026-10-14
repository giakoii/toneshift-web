package vn.toneshift.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Sign-in with an ID token the frontend already obtained from an external provider.
 *
 * @param idToken the provider's ID token, verified by this backend before any account is touched
 */
public record OAuthLoginRequest(@NotBlank String idToken) {
}
