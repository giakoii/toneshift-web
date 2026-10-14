package vn.toneshift.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import vn.toneshift.api.auth.repository.RefreshTokenRepository;
import vn.toneshift.api.support.AbstractIntegrationTest;
import vn.toneshift.api.user.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityRulesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository users;

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @BeforeEach
    void resetDatabase() {
        refreshTokens.deleteAll();
        users.deleteAll();
    }

    @Test
    void rejectsAProtectedEndpointWithoutATokenAsJsonNotHtml() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/user/me"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void rejectsAProtectedEndpointWithAnUnusableToken() throws Exception {
        mockMvc.perform(get("/api/user/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/user/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSignedWithAnotherKey()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/user/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + unsignedToken()))
                .andExpect(status().isUnauthorized());
    }

    /**
     * A public endpoint must still answer a caller whose token has gone bad, rather than failing
     * the whole request with 401.
     */
    @Test
    void treatsAnUnusableTokenOnAPublicEndpointAsAGuest() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"never-issued"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenSignedWithAnotherKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"never-issued"}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void deniesAdminEndpointsToAUserRoleWithForbiddenNotServerError() throws Exception {
        String accessToken = registerAndReturnAccessToken();

        mockMvc.perform(get("/api/admin/reports").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void deniesAdminEndpointsToAGuestWithUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/reports"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // Registers an account and returns its access token.
    private String registerAndReturnAccessToken() throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Khoi PG","email":"khoi@example.com","password":"Passw0rd!x"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    // Builds a structurally valid token signed with a key this backend does not know.
    private String tokenSignedWithAnotherKey() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("toneshift")
                .subject(UUID.randomUUID().toString())
                .claim("role", "ADMIN")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner("an-entirely-different-secret-key-of-32b".getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }

    // Builds an alg=none token, the classic attempt at bypassing signature verification.
    private String unsignedToken() {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(
                "{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(("""
                {"iss":"toneshift","sub":"%s","role":"ADMIN","exp":%d}
                """.formatted(UUID.randomUUID(), Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond()))
                .getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".";
    }
}
