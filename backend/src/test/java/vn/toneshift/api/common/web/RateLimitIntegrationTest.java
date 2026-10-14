package vn.toneshift.api.common.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import vn.toneshift.api.auth.repository.RefreshTokenRepository;
import vn.toneshift.api.support.AbstractIntegrationTest;
import vn.toneshift.api.user.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestPropertySource(properties = "app.rate-limit.login-per-minute=" + RateLimitIntegrationTest.LOGIN_LIMIT_PER_MINUTE)
class RateLimitIntegrationTest extends AbstractIntegrationTest {

    /** Kept small so the test does not pay for ten bcrypt comparisons. */
    static final String LOGIN_LIMIT_PER_MINUTE = "3";

    private static final int LOGIN_LIMIT = Integer.parseInt(LOGIN_LIMIT_PER_MINUTE);

    @Autowired
    private MockMvc mockMvc;

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
     * Without this cap, a wrong password costs an attacker nothing to retry, so the only real
     * limit on guessing would be bcrypt's own cost.
     */
    @Test
    void stopsRepeatedLoginAttemptsFromTheSameAddress() throws Exception {
        for (int attempt = 0; attempt < LOGIN_LIMIT; attempt++) {
            mockMvc.perform(login("someone@example.com"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(login("someone@example.com"))
                .andExpectAll(
                        status().isTooManyRequests(),
                        jsonPath("$.code").value("RATE_LIMITED"),
                        jsonPath("$.traceId").isNotEmpty(),
                        header().exists(HttpHeaders.RETRY_AFTER));
    }

    @Test
    void doesNotLimitEndpointsThatAreNotCredentialChecks() throws Exception {
        for (int attempt = 0; attempt < LOGIN_LIMIT + 5; attempt++) {
            mockMvc.perform(get("/api/user/me")).andExpect(status().isUnauthorized());
        }
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(String email) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"WrongPass1"}
                        """.formatted(email));
    }
}
