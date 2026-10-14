package vn.toneshift.api.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import vn.toneshift.api.support.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthValidationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsAPasswordShorterThanEightCharacters() throws Exception {
        registerWithPassword("Pass1w").andExpectAll(
                status().isBadRequest(),
                jsonPath("$.code").value("VALIDATION_FAILED"),
                jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void rejectsAPasswordWithoutADigit() throws Exception {
        registerWithPassword("PasswordOnly").andExpectAll(
                status().isBadRequest(),
                jsonPath("$.code").value("VALIDATION_FAILED"),
                jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void rejectsAPasswordLongerThanBcryptCanUse() throws Exception {
        registerWithPassword("a1" + "x".repeat(71)).andExpectAll(
                status().isBadRequest(),
                jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void rejectsAMalformedEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Khoi","email":"not-an-email","password":"Passw0rd!x"}
                                """))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.code").value("VALIDATION_FAILED"),
                        jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void rejectsAnEmptyBodyWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.code").value("VALIDATION_FAILED"),
                        jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void reportsBrokenJsonAsMalformedRatherThanValidationFailed() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.code").value("MALFORMED_REQUEST"),
                        jsonPath("$.errors").isEmpty());
    }

    @Test
    void keepsVietnameseDiacriticsIntactInErrorMessages() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"không-hợp-lệ","password":"Passw0rd!x"}
                                """))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.message").value("Dữ liệu gửi lên không hợp lệ"));
    }

    // Posts a registration whose only varying field is the password.
    private org.springframework.test.web.servlet.ResultActions registerWithPassword(String password) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Khoi","email":"validation@example.com","password":"%s"}
                        """.formatted(password)));
    }
}
