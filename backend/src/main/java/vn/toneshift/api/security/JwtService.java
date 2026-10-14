package vn.toneshift.api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import vn.toneshift.api.config.JwtProperties;
import vn.toneshift.api.user.entity.User;

import java.time.Duration;
import java.time.Instant;

/**
 * Mints this backend's access tokens.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    /** Claim carrying the user's role. */
    public static final String CLAIM_ROLE = "role";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    /**
     * Creates an access token for a user.
     *
     * <p>Carries only {@code sub}, {@code role}, {@code iss}, {@code iat} and {@code exp}: no email
     * and nothing else sensitive, because the token travels through the browser.
     *
     * @param user the user to issue the token for
     * @return the signed, compact JWT
     */
    public String createAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTtl()))
                .claim(CLAIM_ROLE, user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Returns how long an issued access token stays valid.
     *
     * @return the access token lifetime
     */
    public Duration accessTtl() {
        return properties.accessTtl();
    }
}
