package vn.toneshift.api.auth;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import vn.toneshift.api.auth.entity.RefreshToken;
import vn.toneshift.api.auth.repository.RefreshTokenRepository;
import vn.toneshift.api.support.AbstractIntegrationTest;
import vn.toneshift.api.user.repository.UserRepository;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives real concurrent requests, which MockMvc cannot: the row lock that serialises rotation
 * only shows up when two requests run on separate connections at the same time.
 */
class RefreshConcurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Passw0rd!x";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository users;

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @BeforeEach
    void resetDatabase() {
        // HttpURLConnection, the default here, tries to re-authenticate on a 401 and fails with
        // "cannot retry due to server authentication" instead of handing over the response body.
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
        refreshTokens.deleteAll();
        users.deleteAll();
    }

    @Test
    void onlyOneOfTwoSimultaneousRotationsSucceedsAndTheSessionSurvives() throws Exception {
        String refreshToken = register("concurrent@example.com");

        List<ResponseEntity<String>> responses = inParallel(
                () -> refresh(refreshToken),
                () -> refresh(refreshToken));

        assertThat(responses).filteredOn(response -> response.getStatusCode() == HttpStatus.OK).hasSize(1);
        assertThat(responses).filteredOn(response -> response.getStatusCode() == HttpStatus.UNAUTHORIZED).hasSize(1);

        // The loser was a retry inside the grace window, so the family must still hold one
        // usable token: the winner's replacement.
        List<RefreshToken> active = refreshTokens.findAll().stream().filter(token -> !token.isRevoked()).toList();
        assertThat(active).hasSize(1);
    }

    @Test
    void onlyOneOfManySimultaneousRegistrationsOfTheSameEmailSucceeds() throws Exception {
        List<Callable<ResponseEntity<String>>> attempts = java.util.stream.IntStream.range(0, 8)
                .<Callable<ResponseEntity<String>>>mapToObj(i -> () -> registerRequest("race@example.com"))
                .toList();

        List<ResponseEntity<String>> responses = inParallel(attempts);

        assertThat(responses).filteredOn(r -> r.getStatusCode() == HttpStatus.CREATED).hasSize(1);
        assertThat(responses).filteredOn(r -> r.getStatusCode() == HttpStatus.CONFLICT)
                .hasSize(attempts.size() - 1);
        assertThat(users.findAll()).hasSize(1);
    }

    // Registers an account and returns its refresh token.
    private String register(String email) throws Exception {
        ResponseEntity<String> response = registerRequest(email);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getBody());
        return body.get("refreshToken").asText();
    }

    private ResponseEntity<String> registerRequest(String email) {
        return restTemplate.postForEntity("/api/auth/register", json("""
                {"name":"Khoi PG","email":"%s","password":"%s"}
                """.formatted(email, PASSWORD)), String.class);
    }

    private ResponseEntity<String> refresh(String refreshToken) {
        return restTemplate.postForEntity("/api/auth/refresh", json("""
                {"refreshToken":"%s"}
                """.formatted(refreshToken)), String.class);
    }

    private HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    @SafeVarargs
    private List<ResponseEntity<String>> inParallel(Callable<ResponseEntity<String>>... tasks) throws Exception {
        return inParallel(List.of(tasks));
    }

    // Runs the calls at the same time and collects their responses.
    private List<ResponseEntity<String>> inParallel(List<Callable<ResponseEntity<String>>> tasks) throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(tasks.size())) {
            List<Future<ResponseEntity<String>>> futures = pool.invokeAll(tasks);
            List<ResponseEntity<String>> responses = new java.util.ArrayList<>();
            for (Future<ResponseEntity<String>> future : futures) {
                responses.add(future.get());
            }
            return responses;
        }
    }
}
