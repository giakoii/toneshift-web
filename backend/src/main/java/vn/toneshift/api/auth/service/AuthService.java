package vn.toneshift.api.auth.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.toneshift.api.auth.dto.AuthResponse;
import vn.toneshift.api.auth.dto.ForgotPasswordRequest;
import vn.toneshift.api.auth.dto.LoginRequest;
import vn.toneshift.api.auth.dto.OAuthLoginRequest;
import vn.toneshift.api.auth.dto.RefreshRequest;
import vn.toneshift.api.auth.dto.RegisterRequest;
import vn.toneshift.api.auth.dto.ResetPasswordRequest;
import vn.toneshift.api.auth.entity.AuthProvider;
import vn.toneshift.api.auth.entity.PasswordResetToken;
import vn.toneshift.api.auth.event.PasswordChangedEvent;
import vn.toneshift.api.auth.event.PasswordResetRequestedEvent;
import vn.toneshift.api.auth.oidc.OidcIdentity;
import vn.toneshift.api.auth.oidc.OidcProviderRegistry;
import vn.toneshift.api.auth.oidc.OidcTokenVerifier;
import vn.toneshift.api.auth.repository.AuthProviderRepository;
import vn.toneshift.api.auth.repository.PasswordResetTokenRepository;
import vn.toneshift.api.auth.repository.RefreshTokenRepository;
import vn.toneshift.api.common.enums.AuthProviderType;
import vn.toneshift.api.common.enums.Role;
import vn.toneshift.api.common.enums.UserStatus;
import vn.toneshift.api.common.event.EventPublisher;
import vn.toneshift.api.common.exception.BusinessRuleException;
import vn.toneshift.api.common.exception.ConflictException;
import vn.toneshift.api.common.exception.ErrorCode;
import vn.toneshift.api.common.exception.ForbiddenException;
import vn.toneshift.api.common.exception.UnauthorizedException;
import vn.toneshift.api.common.logging.AppLog;
import vn.toneshift.api.common.logging.LogMasker;
import vn.toneshift.api.common.util.HashUtils;
import vn.toneshift.api.security.JwtService;
import vn.toneshift.api.user.entity.User;
import vn.toneshift.api.user.mapper.UserMapper;
import vn.toneshift.api.user.repository.UserRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Registration, sign-in and sign-out.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final AppLog log = AppLog.get(AuthService.class);

    /** Width of {@code users.name}. */
    private static final int NAME_MAX_LENGTH = 100;

    /** Width of {@code users.image_url}. */
    private static final int IMAGE_URL_MAX_LENGTH = 500;

    /** How long a password reset link stays valid. */
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final AuthProviderRepository authProviders;
    private final PasswordResetTokenRepository passwordResetTokens;
    private final EventPublisher events;
    private final TokenService tokenService;
    private final OidcProviderRegistry providerRegistry;
    private final OidcTokenVerifier tokenVerifier;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    /**
     * Hash compared against when no real one is available, so a failed sign-in costs the same
     * whether or not the email exists.
     */
    private String dummyPasswordHash;

    /**
     * Precomputes the decoy hash once, using the configured encoder so its cost always matches a
     * real comparison.
     */
    @PostConstruct
    void initDummyPasswordHash() {
        dummyPasswordHash = passwordEncoder.encode(HashUtils.randomToken());
    }

    /**
     * Registers an account with email and password and signs it in.
     *
     * @param request   the registration details
     * @param userAgent the client user agent, may be {@code null}
     * @return the issued tokens and the new user
     * @throws ConflictException if the email is already taken
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, String userAgent) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User user = new User(email, request.name().trim(), Role.USER, UserStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        // Flushed here so two concurrent registrations surface the unique violation inside this
        // call, which the exception handler maps to 409 rather than letting it fail at commit.
        users.saveAndFlush(user);
        log.info("Đăng ký tài khoản mới {}", LogMasker.maskEmail(email));
        return issueTokens(user, userAgent);
    }

    /**
     * Signs in with email and password.
     *
     * @param request   the credentials
     * @param userAgent the client user agent, may be {@code null}
     * @return the issued tokens and the user
     * @throws UnauthorizedException if the credentials do not match
     * @throws ForbiddenException    if the account is blocked
     */
    @Transactional
    public AuthResponse login(LoginRequest request, String userAgent) {
        String email = normalizeEmail(request.email());
        Optional<User> found = users.findByEmail(email);
        if (found.isEmpty() || !found.get().hasPassword()) {
            // Compare against the decoy so the response time does not reveal that the account is
            // missing, or that it can only sign in through a provider.
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
        }
        User user = found.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
        }
        // Checked after the password on purpose: reporting a blocked account to anyone who guesses
        // the email would turn this endpoint into an account enumeration oracle.
        if (user.isBlocked()) {
            throw new ForbiddenException(ErrorCode.ACCOUNT_BLOCKED);
        }
        return issueTokens(user, userAgent);
    }

    /**
     * Signs in with an ID token from an external provider, creating the account on first use.
     *
     * <p>The provider's email is authoritative once verified, so an ID token whose email already
     * belongs to an account links to that account rather than creating a second one.
     *
     * @param providerSlug the provider slug from the request path
     * @param request      the ID token to verify
     * @param userAgent    the client user agent, may be {@code null}
     * @return the issued tokens and the user
     * @throws UnauthorizedException if the token is invalid or its email is unverified
     * @throws ForbiddenException    if the account is blocked
     */
    @Transactional
    public AuthResponse loginWithOidc(String providerSlug, OAuthLoginRequest request, String userAgent) {
        AuthProviderType provider = providerRegistry.require(providerSlug);
        OidcIdentity identity = tokenVerifier.verify(provider, request.idToken());
        if (!identity.emailVerified() || identity.email() == null) {
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
        }
        User user = resolveUser(provider, identity);
        if (user.isBlocked()) {
            throw new ForbiddenException(ErrorCode.ACCOUNT_BLOCKED);
        }
        return issueTokens(user, userAgent);
    }

    /**
     * Exchanges a refresh token for a new token pair.
     *
     * <p>Rotation is single-use: presenting a token that was already rotated fails, and outside
     * the retry grace window it also ends every session in that family.
     *
     * @param request   the refresh token to exchange
     * @param userAgent the client user agent, may be {@code null}
     * @return the new token pair and the user
     * @throws UnauthorizedException if the token is unknown, expired or already used
     * @throws ForbiddenException    if the account is blocked
     */
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse refresh(RefreshRequest request, String userAgent) {
        Rotated rotated = tokenService.rotate(request.refreshToken(), userAgent);
        return AuthResponse.of(
                jwtService.createAccessToken(rotated.user()),
                jwtService.accessTtl().toSeconds(),
                rotated.issued().rawToken(),
                userMapper.toResponse(rotated.user()));
    }

    /**
     * Signs out the session the given refresh token belongs to. Unknown tokens are ignored, so the
     * endpoint is idempotent.
     *
     * @param request the refresh token identifying the session
     */
    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokens.findByTokenHash(HashUtils.sha256Hex(request.refreshToken()))
                // The whole family, not just this token: one family is one device session, so an
                // older member of the chain must not stay usable after signing out.
                .ifPresent(token -> tokenService.revokeFamily(token.getFamilyId()));
    }

    /**
     * Issues a password reset link if the email belongs to an account.
     *
     * <p>Always succeeds, and takes the same shape either way: telling the caller whether an
     * address is registered would leak account existence to anyone.
     *
     * @param request the email to send the link to
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        users.findByEmail(email).ifPresent(user -> {
            Instant now = Instant.now();
            // Requesting a new link retires the previous ones, so only one token is ever live.
            passwordResetTokens.markOutstandingAsUsed(user.getId(), now);
            String rawToken = HashUtils.randomToken();
            Instant expiresAt = now.plus(RESET_TOKEN_TTL);
            passwordResetTokens.save(
                    new PasswordResetToken(user, HashUtils.sha256Hex(rawToken), expiresAt));
            events.publish(new PasswordResetRequestedEvent(
                    user.getId(), email, rawToken, expiresAt, now));
        });
    }

    /**
     * Sets a new password using a reset token, then ends every existing session.
     *
     * @param request the reset token and the new password
     * @throws BusinessRuleException if the token is unknown, expired or already used
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Instant now = Instant.now();
        PasswordResetToken token = passwordResetTokens
                .findByTokenHashForUpdate(HashUtils.sha256Hex(request.token()))
                .filter(candidate -> candidate.isUsableAt(now))
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.RESET_TOKEN_INVALID));
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.setUsedAt(now);
        events.publish(new PasswordChangedEvent(user.getId(), now));
        log.info("Đặt lại mật khẩu thành công cho {}", LogMasker.maskEmail(user.getEmail()));
    }

    // Finds the account the verified identity belongs to, linking or creating as needed.
    private User resolveUser(AuthProviderType provider, OidcIdentity identity) {
        Optional<AuthProvider> existingLink =
                authProviders.findByProviderAndProviderAccountId(provider, identity.subject());
        if (existingLink.isPresent()) {
            // The link wins over the token's email: the email may have changed at the provider,
            // and overwriting it here could collide with another account's unique email.
            return existingLink.get().getUser();
        }
        String email = normalizeEmail(identity.email());
        Optional<User> byEmail = users.findByEmail(email);
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            linkProvider(user, provider, identity.subject());
            log.info("Liên kết {} vào tài khoản {} đã có", provider, LogMasker.maskEmail(email));
            return user;
        }
        return createUserFrom(provider, identity, email);
    }

    // Links an identity to a user, refreshing the subject id if the provider issued a new one.
    private void linkProvider(User user, AuthProviderType provider, String subject) {
        Optional<AuthProvider> sameProvider = authProviders.findByUserIdAndProvider(user.getId(), provider);
        if (sameProvider.isPresent()) {
            // The account was deleted and recreated at the provider, so the verified email now
            // maps to a new subject. Updating keeps sign-in working instead of failing the
            // uniqueness constraint on (user, provider).
            sameProvider.get().setProviderAccountId(subject);
            return;
        }
        user.addProvider(new AuthProvider(user, provider, subject));
    }

    // Creates an account for a first-time provider sign-in; it has no password until the user sets one.
    private User createUserFrom(AuthProviderType provider, OidcIdentity identity, String email) {
        User user = new User(email, displayName(identity, email), Role.USER, UserStatus.ACTIVE);
        user.setEmailVerifiedAt(Instant.now());
        user.setImageUrl(httpsImageUrl(identity.pictureUrl()));
        user.addProvider(new AuthProvider(user, provider, identity.subject()));
        users.saveAndFlush(user);
        log.info("Tạo tài khoản mới từ {} cho {}", provider, LogMasker.maskEmail(email));
        return user;
    }

    // Providers may omit the name or send one longer than the column allows.
    private String displayName(OidcIdentity identity, String email) {
        String name = identity.name() == null || identity.name().isBlank()
                ? email.substring(0, email.indexOf('@'))
                : identity.name().trim();
        return name.length() > NAME_MAX_LENGTH ? name.substring(0, NAME_MAX_LENGTH) : name;
    }

    // Keeps only an https avatar that fits the column, so a provider cannot push mixed content.
    private String httpsImageUrl(String pictureUrl) {
        if (pictureUrl == null || !pictureUrl.startsWith("https://") || pictureUrl.length() > IMAGE_URL_MAX_LENGTH) {
            return null;
        }
        return pictureUrl;
    }

    // Issues a new access and refresh token pair, starting a new session family.
    private AuthResponse issueTokens(User user, String userAgent) {
        IssuedRefreshToken refreshToken = tokenService.issue(user, UUID.randomUUID(), userAgent);
        return AuthResponse.of(
                jwtService.createAccessToken(user),
                jwtService.accessTtl().toSeconds(),
                refreshToken.rawToken(),
                userMapper.toResponse(user));
    }

    // Emails are stored and compared lowercase: the citext unique index is case-insensitive, but
    // parameterised lookups are not, because the driver binds parameters as varchar.
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
