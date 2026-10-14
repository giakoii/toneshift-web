package vn.toneshift.api.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import vn.toneshift.api.auth.oidc.OidcIdentity;
import vn.toneshift.api.auth.oidc.OidcTokenVerifier;
import vn.toneshift.api.auth.repository.AuthProviderRepository;
import vn.toneshift.api.auth.repository.RefreshTokenRepository;
import vn.toneshift.api.common.enums.AuthProviderType;
import vn.toneshift.api.support.AbstractIntegrationTest;
import vn.toneshift.api.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UserProfileIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "profile@example.com";
    private static final String PASSWORD = "Passw0rd!x";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository users;

    @Autowired
    private AuthProviderRepository authProviders;

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @MockBean
    private OidcTokenVerifier tokenVerifier;

    private String accessToken;
    private String refreshToken;

    @BeforeEach
    void registerAndSignIn() throws Exception {
        refreshTokens.deleteAll();
        authProviders.deleteAll();
        users.deleteAll();
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Khoi PG","email":"%s","password":"%s"}
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        accessToken = objectMapper.readTree(body).get("accessToken").asText();
        refreshToken = objectMapper.readTree(body).get("refreshToken").asText();
    }

    @Test
    void updatesTheNameAndAvatar() throws Exception {
        mockMvc.perform(authorized(put("/api/user/profile"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Khoi Pham","image":"https://cdn.example.com/a.png"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Khoi Pham"))
                .andExpect(jsonPath("$.image").value("https://cdn.example.com/a.png"));

        mockMvc.perform(authorized(get("/api/user/me")))
                .andExpect(jsonPath("$.name").value("Khoi Pham"));
    }

    @Test
    void clearsTheAvatarWhenImageIsNull() throws Exception {
        mockMvc.perform(authorized(put("/api/user/profile"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Khoi PG","image":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.image").isEmpty());
    }

    @Test
    void refusesAnAvatarThatIsNotHttps() throws Exception {
        mockMvc.perform(authorized(put("/api/user/profile"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Khoi PG","image":"http://cdn.example.com/a.png"}
                                """))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.code").value("VALIDATION_FAILED"),
                        jsonPath("$.errors[0].field").value("image"));
    }

    @Test
    void changesThePasswordAndSignsOutEveryDevice() throws Exception {
        mockMvc.perform(authorized(post("/api/user/change-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"NewPassw0rd"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isNoContent());

        // Every refresh token is revoked, so the old session cannot be renewed.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());

        // The new password works, the old one does not.
        login("NewPassw0rd").andExpect(status().isOk());
        login(PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void refusesAChangeWithTheWrongCurrentPassword() throws Exception {
        mockMvc.perform(authorized(post("/api/user/change-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"NotMyPassw0rd","newPassword":"NewPassw0rd"}
                                """))
                .andExpectAll(
                        status().isUnauthorized(),
                        jsonPath("$.code").value("INVALID_CREDENTIALS"));

        login(PASSWORD).andExpect(status().isOk());
    }

    /**
     * An account that only ever signed in through a provider has no password to prove, so it may
     * set its first one with a null {@code currentPassword}.
     */
    @Test
    void letsAProviderOnlyAccountSetItsFirstPassword() throws Exception {
        when(tokenVerifier.verify(eq(AuthProviderType.GOOGLE), any())).thenReturn(new OidcIdentity(
                AuthProviderType.GOOGLE, "google-sub-profile", "google-only@example.com", true, "Khoi", null));
        String body = mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"verified-by-mock"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.hasPassword").value(false))
                .andReturn().getResponse().getContentAsString();
        String providerAccessToken = objectMapper.readTree(body).get("accessToken").asText();

        mockMvc.perform(post("/api/user/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + providerAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":null,"newPassword":"FirstPassw0rd"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/user/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + providerAccessToken))
                .andExpect(jsonPath("$.hasPassword").value(true));
        assertThat(users.findByEmail("google-only@example.com")).get()
                .satisfies(user -> assertThat(user.getPasswordHash()).isNotNull());
    }

    @Test
    void rejectsAWeakNewPassword() throws Exception {
        mockMvc.perform(authorized(post("/api/user/change-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"short"}
                                """.formatted(PASSWORD)))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.errors[0].field").value("newPassword"));
    }

    @Test
    void refusesProfileEndpointsWithoutAToken() throws Exception {
        mockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Khoi","image":null}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions login(String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(EMAIL, password)));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authorized(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }
}
