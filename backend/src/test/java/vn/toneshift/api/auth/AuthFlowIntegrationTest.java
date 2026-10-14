package vn.toneshift.api.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.toneshift.api.auth.repository.RefreshTokenRepository;
import vn.toneshift.api.support.AbstractIntegrationTest;
import vn.toneshift.api.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "khoi@example.com";
    private static final String PASSWORD = "Passw0rd!x";

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
    void registersThenSignsInThenReadsOwnProfile() throws Exception {
        MvcResult registered = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Khoi PG","email":"%s","password":"%s"}
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.hasPassword").value(true))
                .andExpect(jsonPath("$.user.providers").isEmpty())
                .andExpect(jsonPath("$.user.createdAt").isNotEmpty())
                .andReturn();

        MvcResult loggedIn = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = readField(loggedIn, "accessToken");
        mockMvc.perform(get("/api/user/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.id").value(readField(registered, "user.id")));
    }

    @Test
    void signsInWithAnyCasingOfTheEmail() throws Exception {
        register();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"KHOI@Example.COM","password":"%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsASecondRegistrationOfTheSameEmailIgnoringCase() throws Exception {
        register();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Khoi PG","email":"KHOI@EXAMPLE.COM","password":"%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.errors").isEmpty())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void reportsTheSameErrorForAnUnknownEmailAndAWrongPassword() throws Exception {
        register();

        String unknownEmail = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@example.com","password":"%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String wrongPassword = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"WrongPass1"}
                                """.formatted(EMAIL)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(readCode(unknownEmail)).isEqualTo("INVALID_CREDENTIALS").isEqualTo(readCode(wrongPassword));
        assertThat(readMessage(unknownEmail)).isEqualTo(readMessage(wrongPassword));
    }

    @Test
    void logoutRevokesTheSessionAndIsIdempotent() throws Exception {
        MvcResult registered = register();
        String refreshToken = readField(registered, "refreshToken");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isNoContent());

        assertThat(refreshTokens.findAll()).singleElement()
                .satisfies(token -> assertThat(token.getRevokedAt()).isNotNull());

        // Repeating it, and sending a token that was never issued, must still succeed.
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"never-issued"}
                                """))
                .andExpect(status().isNoContent());
    }

    // Registers the standard test account and returns the response.
    private MvcResult register() throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Khoi PG","email":"%s","password":"%s"}
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    // Reads a dotted field path out of a JSON response body.
    private String readField(MvcResult result, String path) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        for (String segment : path.split("\\.")) {
            node = node.get(segment);
        }
        return node.asText();
    }

    // Reads the error code out of an error body.
    private String readCode(String body) throws Exception {
        return objectMapper.readTree(body).get("code").asText();
    }

    // Reads the error message out of an error body.
    private String readMessage(String body) throws Exception {
        return objectMapper.readTree(body).get("message").asText();
    }
}
