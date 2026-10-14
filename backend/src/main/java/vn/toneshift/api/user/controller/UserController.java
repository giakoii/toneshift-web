package vn.toneshift.api.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.toneshift.api.common.web.CurrentUser;
import vn.toneshift.api.user.dto.ChangePasswordRequest;
import vn.toneshift.api.user.dto.UpdateProfileRequest;
import vn.toneshift.api.user.dto.UserResponse;
import vn.toneshift.api.user.service.UserService;

import java.util.UUID;

/**
 * Endpoints for the signed-in user's own account.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    /**
     * Returns the signed-in user's profile.
     *
     * @param userId the id of the signed-in user
     * @return the user profile
     */
    @GetMapping("/me")
    @Operation(summary = "Thông tin tài khoản đang đăng nhập")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thông tin tài khoản"),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
    })
    public UserResponse me(@CurrentUser UUID userId) {
        return userService.getMe(userId);
    }

    /**
     * Updates the signed-in user's display name and avatar.
     *
     * @param userId  the id of the signed-in user
     * @param request the new profile values
     * @return the updated profile
     */
    @PutMapping("/profile")
    @Operation(summary = "Cập nhật tên và ảnh đại diện")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thông tin sau khi cập nhật"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED"),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
    })
    public UserResponse updateProfile(@CurrentUser UUID userId,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(userId, request);
    }

    /**
     * Sets or changes the signed-in user's password.
     *
     * @param userId  the id of the signed-in user
     * @param request the current and new password
     */
    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Đổi mật khẩu",
            description = "Đăng xuất mọi thiết bị. Tài khoản chỉ đăng nhập qua provider gửi currentPassword null "
                    + "để đặt mật khẩu lần đầu.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Đã đổi mật khẩu, mọi refresh token bị thu hồi"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED"),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED hoặc INVALID_CREDENTIALS")
    })
    public void changePassword(@CurrentUser UUID userId,
                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
    }
}
