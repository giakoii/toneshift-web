package vn.toneshift.api.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.toneshift.api.auth.event.PasswordChangedEvent;
import vn.toneshift.api.common.event.EventPublisher;
import vn.toneshift.api.common.exception.ErrorCode;
import vn.toneshift.api.common.exception.NotFoundException;
import vn.toneshift.api.common.exception.UnauthorizedException;
import vn.toneshift.api.user.dto.ChangePasswordRequest;
import vn.toneshift.api.user.dto.UpdateProfileRequest;
import vn.toneshift.api.user.dto.UserResponse;
import vn.toneshift.api.user.entity.User;
import vn.toneshift.api.user.mapper.UserMapper;
import vn.toneshift.api.user.repository.UserRepository;

import java.time.Instant;
import java.util.UUID;

/**
 * Default {@link UserService}.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository users;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMe(UUID userId) {
        return userMapper.toResponse(requireUser(userId));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        user.setName(request.name().trim());
        user.setImageUrl(request.image());
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        // An account created through a provider has no password yet, so it sets its first one
        // without proving a previous one.
        if (user.hasPassword() && !matchesCurrentPassword(request, user)) {
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        events.publish(new PasswordChangedEvent(userId, Instant.now()));
    }

    // Loads the user with its provider links, which every response includes.
    private User requireUser(UUID userId) {
        return users.findDetailById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    // Checks the supplied current password against the stored hash.
    private boolean matchesCurrentPassword(ChangePasswordRequest request, User user) {
        return request.currentPassword() != null
                && !request.currentPassword().isBlank()
                && passwordEncoder.matches(request.currentPassword(), user.getPasswordHash());
    }
}
