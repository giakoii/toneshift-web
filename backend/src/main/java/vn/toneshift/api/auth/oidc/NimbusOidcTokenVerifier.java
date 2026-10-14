package vn.toneshift.api.auth.oidc;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.toneshift.api.common.enums.AuthProviderType;
import vn.toneshift.api.common.exception.ErrorCode;
import vn.toneshift.api.common.exception.UnauthorizedException;
import vn.toneshift.api.common.logging.AppLog;

import java.text.ParseException;

/**
 * Verifies ID tokens with the OIDC SDK's {@link com.nimbusds.openid.connect.sdk.validators.IDTokenValidator}.
 *
 * <p>One {@code validate} call covers the signature, the issuer, the audience (including the
 * {@code azp} rule when the token is addressed to several audiences), the expiry and the required
 * claims. The audience check is the one that matters most: without it a token minted for a
 * different application at the same provider would be accepted here.
 */
@Component
@RequiredArgsConstructor
public class NimbusOidcTokenVerifier implements OidcTokenVerifier {

    private static final AppLog log = AppLog.get(NimbusOidcTokenVerifier.class);

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_EMAIL_VERIFIED = "email_verified";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_PICTURE = "picture";

    private final OidcProviderRegistry registry;

    @Override
    public OidcIdentity verify(AuthProviderType provider, String idToken) {
        IDTokenClaimsSet claims = validate(provider, idToken);
        return new OidcIdentity(
                provider,
                claims.getSubject().getValue(),
                claims.getStringClaim(CLAIM_EMAIL),
                emailVerified(claims),
                claims.getStringClaim(CLAIM_NAME),
                claims.getStringClaim(CLAIM_PICTURE));
    }

    // Parses and validates the token, reporting any failure as invalid credentials.
    private IDTokenClaimsSet validate(AuthProviderType provider, String idToken) {
        try {
            JWT parsed = JWTParser.parse(idToken);
            // No nonce: the frontend runs the OAuth flow and this backend only receives the
            // resulting token, so there is no nonce of ours to compare against.
            return registry.validatorFor(provider).validate(parsed, null);
        } catch (ParseException | BadJOSEException | JOSEException e) {
            // The reason only, never the token itself.
            log.debug("ID token của {} không hợp lệ: {}", provider, e.getMessage());
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    // Reads email_verified, which some providers send as a string rather than a boolean.
    private boolean emailVerified(IDTokenClaimsSet claims) {
        Boolean asBoolean = claims.getBooleanClaim(CLAIM_EMAIL_VERIFIED);
        if (asBoolean != null) {
            return asBoolean;
        }
        return Boolean.parseBoolean(claims.getStringClaim(CLAIM_EMAIL_VERIFIED));
    }
}
