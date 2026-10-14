package vn.toneshift.api.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import vn.toneshift.api.auth.oidc.OidcIdentity;
import vn.toneshift.api.auth.oidc.OidcTokenVerifier;
import vn.toneshift.api.auth.repository.AuthProviderRepository;
import vn.toneshift.api.auth.repository.RefreshTokenRepository;
import vn.toneshift.api.common.enums.AuthProviderType;
import vn.toneshift.api.common.enums.Role;
import vn.toneshift.api.common.enums.UserStatus;
import vn.toneshift.api.support.AbstractIntegrationTest;
import vn.toneshift.api.user.entity.User;
import vn.toneshift.api.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class GoogleLoginIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "khoi@example.com";
    private static final String SUBJECT = "google-sub-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private AuthProviderRepository authProviders;

    @Autowired
    private RefreshTokenRepository refreshTokens;

    /** The ID token is verified in its own unit test; here the identity is handed over directly. */
    @MockBean
    private OidcTokenVerifier tokenVerifier;

    @BeforeEach
    void resetDatabase() {
        refreshTokens.deleteAll();
        authProviders.deleteAll();
        users.deleteAll();
    }

    @Test
    void createsAnAccountWithoutAPasswordOnFirstGoogleSignIn() throws Exception {
        givenIdentity(identity(EMAIL, SUBJECT, true));

        mockMvc.perform(googleLogin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.name").value("Khoi PG"))
                .andExpect(jsonPath("$.user.hasPassword").value(false))
                .andExpect(jsonPath("$.user.providers[0]").value("GOOGLE"));

        assertThat(users.findByEmail(EMAIL)).get().satisfies(user -> {
            assertThat(user.getPasswordHash()).isNull();
            assertThat(user.getEmailVerifiedAt()).isNotNull();
            assertThat(user.getRole()).isEqualTo(Role.USER);
        });
    }

    /**
     * The provider has verified the email, so it identifies an existing account rather than
     * creating a second one for the same person.
     */
    @Test
    void linksToTheExistingAccountWithTheSameEmailInsteadOfCreatingAnother() throws Exception {
        registerWithPassword();
        givenIdentity(identity(EMAIL, SUBJECT, true));

        mockMvc.perform(googleLogin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.hasPassword").value(true))
                .andExpect(jsonPath("$.user.providers[0]").value("GOOGLE"));

        assertThat(users.findAll()).hasSize(1);
        assertThat(authProviders.findAll()).hasSize(1);
    }

    @Test
    void reusesTheSameAccountOnEveryLaterSignIn() throws Exception {
        givenIdentity(identity(EMAIL, SUBJECT, true));

        mockMvc.perform(googleLogin()).andExpect(status().isOk());
        mockMvc.perform(googleLogin()).andExpect(status().isOk());

        assertThat(users.findAll()).hasSize(1);
        assertThat(authProviders.findAll()).hasSize(1);
    }

    /**
     * The account was deleted and recreated at the provider, so the verified email now maps to a
     * new subject. The link is updated rather than failing the (user, provider) uniqueness rule.
     */
    @Test
    void updatesTheStoredSubjectWhenTheProviderIssuesANewOne() throws Exception {
        givenIdentity(identity(EMAIL, SUBJECT, true));
        mockMvc.perform(googleLogin()).andExpect(status().isOk());

        givenIdentity(identity(EMAIL, "google-sub-recreated", true));
        mockMvc.perform(googleLogin()).andExpect(status().isOk());

        assertThat(users.findAll()).hasSize(1);
        assertThat(authProviders.findAll()).singleElement().satisfies(link ->
                assertThat(link.getProviderAccountId()).isEqualTo("google-sub-recreated"));
    }

    @Test
    void refusesAnIdentityWhoseEmailTheProviderHasNotVerified() throws Exception {
        givenIdentity(identity(EMAIL, SUBJECT, false));

        mockMvc.perform(googleLogin())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        assertThat(users.findAll()).isEmpty();
    }

    @Test
    void refusesToSignInABlockedAccount() throws Exception {
        User blocked = new User(EMAIL, "Khoi PG", Role.USER, UserStatus.BLOCKED);
        users.saveAndFlush(blocked);
        givenIdentity(identity(EMAIL, SUBJECT, true));

        mockMvc.perform(googleLogin())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_BLOCKED"));
    }

    @Test
    void reportsAnUnsupportedProviderAsABadRequestNotAServerError() throws Exception {
        mockMvc.perform(post("/api/auth/oauth/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"any-token"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void rejectsARequestWithoutAnIdToken() throws Exception {
        mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.code").value("VALIDATION_FAILED"),
                        jsonPath("$.errors[0].field").value("idToken"));
    }

    // Makes the verifier return the given identity for any Google token.
    private void givenIdentity(OidcIdentity identity) {
        when(tokenVerifier.verify(eq(AuthProviderType.GOOGLE), any())).thenReturn(identity);
    }

    // Builds an identity as a provider would assert it.
    private OidcIdentity identity(String email, String subject, boolean emailVerified) {
        return new OidcIdentity(AuthProviderType.GOOGLE, subject, email, emailVerified,
                "Khoi PG", "https://example.com/avatar.png");
    }

    // Posts a Google sign-in with a placeholder token, since the verifier is mocked.
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder googleLogin() {
        return post("/api/auth/oauth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"idToken":"verified-by-mock"}
                        """);
    }

    // Registers the same email through the credentials flow.
    private void registerWithPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Khoi PG","email":"%s","password":"Passw0rd!x"}
                                """.formatted(EMAIL)))
                .andExpect(status().isCreated());
    }
}
