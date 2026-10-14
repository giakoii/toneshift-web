package vn.toneshift.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import vn.toneshift.api.auth.entity.PasswordResetToken;
import vn.toneshift.api.auth.event.PasswordResetRequestedEvent;
import vn.toneshift.api.auth.repository.PasswordResetTokenRepository;
import vn.toneshift.api.auth.repository.RefreshTokenRepository;
import vn.toneshift.api.support.AbstractIntegrationTest;
import vn.toneshift.api.user.repository.UserRepository;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@RecordApplicationEvents
class PasswordResetIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "reset@example.com";
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
    private PasswordResetTokenRepository resetTokens;

    @Autowired
    private ApplicationEvents events;

    @MockBean
    private JavaMailSender mailSender;

    private String refreshToken;

    @BeforeEach
    void registerAccount() throws Exception {
        resetTokens.deleteAll();
        refreshTokens.deleteAll();
        users.deleteAll();
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Khoi PG","email":"%s","password":"%s"}
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        refreshToken = objectMapper.readTree(body).get("refreshToken").asText();
    }

    @Test
    void mailsAResetLinkAndLetsThePasswordBeChangedOnce() throws Exception {
        forgotPassword(EMAIL).andExpect(status().isAccepted());

        String token = tokenFromMail();

        resetPassword(token, "BrandNewPass1").andExpect(status().isNoContent());

        // The new password works and the old one does not.
        login("BrandNewPass1").andExpect(status().isOk());
        login(PASSWORD).andExpect(status().isUnauthorized());
        // Resetting with the same link again fails: the token is single use.
        resetPassword(token, "AnotherPass1")
                .andExpectAll(
                        status().isUnprocessableEntity(),
                        jsonPath("$.code").value("RESET_TOKEN_INVALID"));
    }

    @Test
    void endsEverySessionAfterAReset() throws Exception {
        forgotPassword(EMAIL).andExpect(status().isAccepted());
        resetPassword(tokenFromMail(), "BrandNewPass1").andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Answering differently for an unknown address would let anyone test which emails are
     * registered, so the response is identical and no mail is sent.
     */
    @Test
    void acceptsAnUnknownEmailWithoutSendingAnything() throws Exception {
        forgotPassword("nobody@example.com").andExpect(status().isAccepted());

        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
        assertThat(events.stream(PasswordResetRequestedEvent.class)).isEmpty();
        assertThat(resetTokens.findAll()).isEmpty();
    }

    @Test
    void refusesAnExpiredToken() throws Exception {
        forgotPassword(EMAIL).andExpect(status().isAccepted());
        String token = tokenFromMail();
        expireStoredToken();

        resetPassword(token, "BrandNewPass1")
                .andExpectAll(
                        status().isUnprocessableEntity(),
                        jsonPath("$.code").value("RESET_TOKEN_INVALID"));
        login(PASSWORD).andExpect(status().isOk());
    }

    @Test
    void refusesAnUnknownToken() throws Exception {
        resetPassword("never-issued", "BrandNewPass1")
                .andExpectAll(
                        status().isUnprocessableEntity(),
                        jsonPath("$.code").value("RESET_TOKEN_INVALID"));
    }

    /**
     * Only the newest link may work, so an older one that was already mailed cannot be used after
     * the user asks again.
     */
    @Test
    void retiresTheEarlierLinkWhenANewOneIsRequested() throws Exception {
        forgotPassword(EMAIL).andExpect(status().isAccepted());
        String firstToken = tokenFromMail();

        forgotPassword(EMAIL).andExpect(status().isAccepted());

        resetPassword(firstToken, "BrandNewPass1")
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rejectsAWeakNewPassword() throws Exception {
        forgotPassword(EMAIL).andExpect(status().isAccepted());

        resetPassword(tokenFromMail(), "weak")
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.errors[0].field").value("newPassword"));
    }

    // Reads the reset token out of the mail the async listener sent.
    private String tokenFromMail() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        // The listener runs after commit on another thread, so the send is awaited.
        verify(mailSender, timeout(2000)).send(captor.capture());
        String text = captor.getValue().getText();
        assertThat(text).isNotNull();
        String marker = "token=";
        String tail = text.substring(text.indexOf(marker) + marker.length());
        return URLDecoder.decode(tail.substring(0, tail.indexOf('\n')).trim(), StandardCharsets.UTF_8);
    }

    // Moves the stored token's expiry into the past.
    private void expireStoredToken() {
        PasswordResetToken token = resetTokens.findAll().getFirst();
        token.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        resetTokens.saveAndFlush(token);
    }

    private org.springframework.test.web.servlet.ResultActions forgotPassword(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s"}
                        """.formatted(email)));
    }

    private org.springframework.test.web.servlet.ResultActions resetPassword(String token, String newPassword)
            throws Exception {
        return mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new vn.toneshift.api.auth.dto.ResetPasswordRequest(token, newPassword)))
                .contentType(MediaType.APPLICATION_JSON));
    }

    private org.springframework.test.web.servlet.ResultActions login(String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(EMAIL, password)));
    }
}
