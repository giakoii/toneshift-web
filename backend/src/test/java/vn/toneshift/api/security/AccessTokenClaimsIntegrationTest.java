package vn.toneshift.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import vn.toneshift.api.auth.repository.RefreshTokenRepository;
import vn.toneshift.api.support.AbstractIntegrationTest;
import vn.toneshift.api.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AccessTokenClaimsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private UserRepository users;

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @BeforeEach
    void resetDatabase() {
        refreshTokens.deleteAll();
        users.deleteAll();
    }

    /**
     * The access token travels through the browser, so it must carry nothing beyond what
     * authorization needs: no email, no name, no provider list.
     */
    @Test
    void issuesATokenCarryingOnlyTheClaimsAuthorizationNeeds() throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Khoi PG","email":"claims@example.com","password":"Passw0rd!x"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(body).get("accessToken").asText();
        String userId = objectMapper.readTree(body).get("user").get("id").asText();

        Jwt jwt = jwtDecoder.decode(accessToken);

        assertThat(jwt.getClaims()).containsOnlyKeys("iss", "sub", "iat", "exp", "role");
        assertThat(jwt.getSubject()).isEqualTo(userId);
        assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
        // Read as a plain string: iss is a StringOrURI and this one is not a URL, so the
        // getIssuer() convenience method cannot be used.
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("toneshift");
        assertThat(jwt.getExpiresAt()).isNotNull();
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plusSeconds(900));
    }
}
