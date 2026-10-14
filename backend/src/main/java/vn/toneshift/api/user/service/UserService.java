package vn.toneshift.api.user.service;

import vn.toneshift.api.user.dto.ChangePasswordRequest;
import vn.toneshift.api.user.dto.UpdateProfileRequest;
import vn.toneshift.api.user.dto.UserResponse;

import java.util.UUID;

/**
 * Reads and updates the signed-in user's own profile.
 */
public interface UserService {

    /**
     * Returns the profile of the signed-in user.
     *
     * @param userId the id of the signed-in user
     * @return the user profile
     */
    UserResponse getMe(UUID userId);

    /**
     * Updates the signed-in user's display name and avatar.
     *
     * @param userId  the id of the signed-in user
     * @param request the new profile values
     * @return the updated profile
     */
    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);

    /**
     * Sets or changes the signed-in user's password, ending every existing session.
     *
     * @param userId  the id of the signed-in user
     * @param request the current and new password
     */
    void changePassword(UUID userId, ChangePasswordRequest request);
}
