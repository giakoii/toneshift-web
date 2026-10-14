package vn.toneshift.api.auth.dto;

import vn.toneshift.api.user.dto.UserResponse;

/**
 * The token pair and user returned by every successful sign-in.
 *
 * @param accessToken  the signed access token
 * @param tokenType    always {@code Bearer}
 * @param expiresIn    access token lifetime in seconds
 * @param refreshToken the opaque refresh token, valid once
 * @param user         the signed-in user
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        UserResponse user) {

    /** The only token type this API issues. */
    public static final String BEARER = "Bearer";

    /**
     * Builds a response for an issued token pair.
     *
     * @param accessToken  the signed access token
     * @param expiresIn    access token lifetime in seconds
     * @param refreshToken the raw refresh token
     * @param user         the signed-in user
     * @return the authentication response
     */
    public static AuthResponse of(String accessToken, long expiresIn, String refreshToken, UserResponse user) {
        return new AuthResponse(accessToken, BEARER, expiresIn, refreshToken, user);
    }
}
