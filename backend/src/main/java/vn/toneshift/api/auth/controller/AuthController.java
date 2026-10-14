package vn.toneshift.api.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.toneshift.api.auth.dto.AuthResponse;
import vn.toneshift.api.auth.dto.ForgotPasswordRequest;
import vn.toneshift.api.auth.dto.LoginRequest;
import vn.toneshift.api.auth.dto.OAuthLoginRequest;
import vn.toneshift.api.auth.dto.RefreshRequest;
import vn.toneshift.api.auth.dto.RegisterRequest;
import vn.toneshift.api.auth.dto.ResetPasswordRequest;
import vn.toneshift.api.auth.service.AuthService;

/**
 * Public authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registers an account with email and password.
     *
     * @param request   the registration details
     * @param userAgent the client user agent, recorded with the session
     * @return the issued tokens and the new user
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Đăng ký tài khoản bằng email và mật khẩu")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Đăng ký thành công"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED hoặc MALFORMED_REQUEST"),
            @ApiResponse(responseCode = "409", description = "EMAIL_ALREADY_EXISTS")
    })
    public AuthResponse register(@Valid @RequestBody RegisterRequest request,
                                 @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return authService.register(request, userAgent);
    }

    /**
     * Signs in with email and password.
     *
     * @param request   the credentials
     * @param userAgent the client user agent, recorded with the session
     * @return the issued tokens and the user
     */
    @PostMapping("/login")
    @Operation(summary = "Đăng nhập bằng email và mật khẩu")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công"),
            @ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS"),
            @ApiResponse(responseCode = "403", description = "ACCOUNT_BLOCKED")
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request,
                              @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return authService.login(request, userAgent);
    }

    /**
     * Signs in with an ID token from an external provider.
     *
     * @param provider  the provider slug, for example {@code google}
     * @param request   the ID token obtained by the frontend
     * @param userAgent the client user agent, recorded with the session
     * @return the issued tokens and the user
     */
    @PostMapping("/oauth/{provider}")
    @Operation(summary = "Đăng nhập bằng ID token của nhà cung cấp OIDC",
            description = "{provider} là slug đã cấu hình trong app.oidc.providers, hiện có: google")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công"),
            @ApiResponse(responseCode = "400", description = "MALFORMED_REQUEST khi provider không được hỗ trợ"),
            @ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS khi ID token sai hoặc email chưa xác thực"),
            @ApiResponse(responseCode = "403", description = "ACCOUNT_BLOCKED")
    })
    public AuthResponse loginWithOidc(@PathVariable String provider,
                                      @Valid @RequestBody OAuthLoginRequest request,
                                      @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return authService.loginWithOidc(provider, request, userAgent);
    }

    /**
     * Exchanges a refresh token for a new token pair.
     *
     * @param request   the refresh token to exchange
     * @param userAgent the client user agent, recorded with the session
     * @return the new token pair and the user
     */
    @PostMapping("/refresh")
    @Operation(summary = "Làm mới access token, refresh token được rotate")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cặp token mới"),
            @ApiResponse(responseCode = "401", description = "REFRESH_TOKEN_INVALID; token dùng lại sẽ thu hồi cả family"),
            @ApiResponse(responseCode = "403", description = "ACCOUNT_BLOCKED")
    })
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request,
                                @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return authService.refresh(request, userAgent);
    }

    /**
     * Requests a password reset link.
     *
     * @param request the email to send the link to
     */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Gửi liên kết đặt lại mật khẩu",
            description = "Luôn trả 202 dù email có tồn tại hay không, để không tiết lộ email nào đã đăng ký.")
    @ApiResponse(responseCode = "202", description = "Đã nhận yêu cầu")
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
    }

    /**
     * Sets a new password using a reset token.
     *
     * @param request the reset token and the new password
     */
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Đặt mật khẩu mới bằng token trong email",
            description = "Thành công sẽ thu hồi toàn bộ refresh token của tài khoản.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Đã đặt mật khẩu mới"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED"),
            @ApiResponse(responseCode = "422", description = "RESET_TOKEN_INVALID")
    })
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
    }

    /**
     * Signs out the session the refresh token belongs to. Idempotent.
     *
     * @param request the refresh token identifying the session
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Đăng xuất, thu hồi session của refresh token")
    @ApiResponse(responseCode = "204", description = "Đã đăng xuất, kể cả khi token không tồn tại")
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
    }
}
