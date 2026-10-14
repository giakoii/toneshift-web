package vn.toneshift.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import vn.toneshift.api.auth.entity.RefreshToken;
import vn.toneshift.api.auth.event.RefreshTokenReuseDetectedEvent;
import vn.toneshift.api.auth.repository.RefreshTokenRepository;
import vn.toneshift.api.common.enums.UserStatus;
import vn.toneshift.api.common.util.HashUtils;
import vn.toneshift.api.support.AbstractIntegrationTest;
import vn.toneshift.api.user.entity.User;
import vn.toneshift.api.user.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@RecordApplicationEvents
class RefreshRotationIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "rotate@example.com";
    private static final String PASSWORD = "Passw0rd!x";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository users;

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @Autowired
    private ApplicationEvents events;

    @BeforeEach
    void resetDatabase() {
        refreshTokens.deleteAll();
        users.deleteAll();
    }

    @Test
    void issuesANewPairAndRetiresThePresentedToken() throws Exception {
        String first = register();

        String second = refresh(first);

        assertThat(second).isNotEqualTo(first);
        assertThat(refreshTokens.findByTokenHash(HashUtils.sha256Hex(first)))
                .get()
                .satisfies(token -> {
                    assertThat(token.getRevokedAt()).isNotNull();
                    assertThat(token.getReplacedBy()).isNotNull();
                });
        // The replacement stays in the same session family.
        assertThat(refreshTokens.findAll()).extracting(RefreshToken::getFamilyId).containsOnly(familyId());
    }

    /**
     * A client whose rotation response was lost retries with the same token. That must fail, but
     * must not sign the user out of a working session.
     */
    @Test
    void refusesAReplayInsideTheGraceWindowWithoutEndingTheSession() throws Exception {
        String first = register();
        String second = refresh(first);

        mockMvc.perform(refreshRequest(first))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));

        assertThat(events.stream(RefreshTokenReuseDetectedEvent.class)).isEmpty();
        assertThat(activeTokenHashes()).hasSize(1);
        // The session still works.
        assertThat(refresh(second)).isNotBlank();
    }

    /**
     * The revocation must outlive the rejected request: it runs in its own transaction, because
     * the surrounding one rolls back when the 401 is thrown.
     */
    @Test
    void endsTheWholeFamilyWhenAReplayArrivesAfterTheGraceWindow() throws Exception {
        String first = register();
        String second = refresh(first);
        backdateRevocation(first);

        mockMvc.perform(refreshRequest(first))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));

        assertThat(activeTokenHashes()).isEmpty();
        assertThat(events.stream(RefreshTokenReuseDetectedEvent.class)).hasSize(1);
        // The replacement token was revoked along with the rest of the family.
        mockMvc.perform(refreshRequest(second)).andExpect(status().isUnauthorized());
    }

    @Test
    void refusesAnUnknownOrExpiredToken() throws Exception {
        register();

        mockMvc.perform(refreshRequest("never-issued"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));

        String expired = issueExpiredToken();
        mockMvc.perform(refreshRequest(expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));
    }

    @Test
    void refusesToRefreshABlockedAccount() throws Exception {
        String refreshToken = register();
        User user = users.findByEmail(EMAIL).orElseThrow();
        user.setStatus(UserStatus.BLOCKED);
        users.saveAndFlush(user);

        mockMvc.perform(refreshRequest(refreshToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_BLOCKED"));
    }

    @Test
    void refusesARefreshTokenAfterLogout() throws Exception {
        String refreshToken = register();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(refreshRequest(refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));
    }

    // Registers the test account and returns its refresh token.
    private String register() throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Khoi PG","email":"%s","password":"%s"}
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("refreshToken").asText();
    }

    // Rotates the given refresh token and returns the replacement.
    private String refresh(String refreshToken) throws Exception {
        String body = mockMvc.perform(refreshRequest(refreshToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("refreshToken").asText();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder refreshRequest(String refreshToken) {
        return post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken":"%s"}
                        """.formatted(refreshToken));
    }

    // Moves a token's revocation far enough into the past that a replay is no longer a retry.
    private void backdateRevocation(String rawToken) {
        RefreshToken token = refreshTokens.findByTokenHash(HashUtils.sha256Hex(rawToken)).orElseThrow();
        token.setRevokedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        refreshTokens.saveAndFlush(token);
    }

    // Stores an already expired token for the test account.
    private String issueExpiredToken() {
        User user = users.findByEmail(EMAIL).orElseThrow();
        String raw = HashUtils.randomToken();
        refreshTokens.saveAndFlush(new RefreshToken(
                user,
                HashUtils.sha256Hex(raw),
                UUID.randomUUID(),
                Instant.now().minus(1, ChronoUnit.DAYS),
                "JUnit"));
        return raw;
    }

    private java.util.List<String> activeTokenHashes() {
        return refreshTokens.findAll().stream()
                .filter(token -> !token.isRevoked())
                .map(RefreshToken::getTokenHash)
                .toList();
    }

    private UUID familyId() {
        return refreshTokens.findAll().getFirst().getFamilyId();
    }
}
