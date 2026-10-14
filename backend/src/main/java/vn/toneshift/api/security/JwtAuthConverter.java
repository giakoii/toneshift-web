package vn.toneshift.api.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import vn.toneshift.api.common.enums.Role;

import java.util.List;

/**
 * Turns a verified access token into an authentication whose name is the user id and whose
 * authority is derived from the {@code role} claim.
 */
@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String AUTHORITY_PREFIX = "ROLE_";

    /**
     * Converts a verified token into an authentication.
     *
     * @param jwt the verified access token
     * @return the authentication for the request
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, authorities(jwt), jwt.getSubject());
    }

    // Maps the role claim to a single authority; an unknown or missing role grants none,
    // so a forged or outdated token cannot reach a role-protected endpoint.
    private List<GrantedAuthority> authorities(Jwt jwt) {
        String claim = jwt.getClaimAsString(JwtService.CLAIM_ROLE);
        if (claim == null) {
            return List.of();
        }
        for (Role role : Role.values()) {
            if (role.name().equals(claim)) {
                return List.of(new SimpleGrantedAuthority(AUTHORITY_PREFIX + role.name()));
            }
        }
        return List.of();
    }
}
