package vn.toneshift.api.song;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.toneshift.api.auth.repository.AuthProviderRepository;
import vn.toneshift.api.auth.repository.RefreshTokenRepository;
import vn.toneshift.api.song.repository.SongRepository;
import vn.toneshift.api.support.AbstractIntegrationTest;
import vn.toneshift.api.user.repository.UserRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class LibraryIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Passw0rd!x";
    private static final String SONG_BODY = """
            {"title":"Nơi này có anh","artist":"Sơn Tùng M-TP",
             "content":"C       G\\nNơi này có anh","originalKey":"C#","targetKey":"D"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SongRepository songs;

    @Autowired
    private UserRepository users;

    @Autowired
    private AuthProviderRepository authProviders;

    @Autowired
    private RefreshTokenRepository refreshTokens;

    private String tokenA;
    private String tokenB;

    @BeforeEach
    void registerTwoUsers() throws Exception {
        songs.deleteAll();
        refreshTokens.deleteAll();
        authProviders.deleteAll();
        users.deleteAll();
        tokenA = register("a-" + UUID.randomUUID() + "@example.com");
        tokenB = register("b-" + UUID.randomUUID() + "@example.com");
    }

    @Test
    void createsASongAndReturnsItsLocation() throws Exception {
        var result = mockMvc.perform(as(tokenA, post("/api/library"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SONG_BODY))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.originalKey").value("C#"),
                        jsonPath("$.targetKey").value("D"),
                        jsonPath("$.visibility").value("PRIVATE"),
                        jsonPath("$.slug").doesNotExist(),
                        jsonPath("$.publishedAt").doesNotExist(),
                        jsonPath("$.version").value(0),
                        jsonPath("$.ratingAvg").value(0.00),
                        jsonPath("$.myScore").doesNotExist())
                .andReturn();
        String id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).endsWith("/api/library/" + id);
    }

    @Test
    void rejectsAnUnknownOrMiscasedKey() throws Exception {
        for (String key : new String[] {"H", "c#", "Db"}) {
            mockMvc.perform(as(tokenA, post("/api/library"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title":"X","content":"C","originalKey":"%s","targetKey":"C"}
                                    """.formatted(key)))
                    .andExpectAll(
                            status().isBadRequest(),
                            jsonPath("$.code").value("MALFORMED_REQUEST"));
        }
    }

    @Test
    void enforcesTheContentLimit() throws Exception {
        mockMvc.perform(as(tokenA, post("/api/library"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithContent("C".repeat(50_000))))
                .andExpect(status().isCreated());

        mockMvc.perform(as(tokenA, post("/api/library"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithContent("C".repeat(50_001))))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.code").value("VALIDATION_FAILED"),
                        jsonPath("$.errors[0].field").value("content"));
    }

    @Test
    void rejectsABlankTitle() throws Exception {
        mockMvc.perform(as(tokenA, post("/api/library"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"   ","content":"C","originalKey":"C","targetKey":"C"}
                                """))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    void listsOnlyMySongsNewestFirstWithoutContent() throws Exception {
        createSong(tokenA, "Bài 1");
        createSong(tokenA, "Bài 2");
        createSong(tokenA, "Bài 3");
        createSong(tokenB, "Bài của B");

        mockMvc.perform(as(tokenA, get("/api/library")))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.total").value(3),
                        jsonPath("$.page").value(1),
                        jsonPath("$.limit").value(20),
                        jsonPath("$.data[0].content").doesNotExist(),
                        jsonPath("$.data[0].excerpt").isString());
    }

    @Test
    void filtersAndPaginates() throws Exception {
        createSong(tokenA, "Nơi này có anh");
        createSong(tokenA, "Chúng ta của hiện tại");

        mockMvc.perform(as(tokenA, get("/api/library").param("q", "Nơi")))
                .andExpectAll(status().isOk(), jsonPath("$.total").value(1));

        mockMvc.perform(as(tokenA, get("/api/library").param("page", "2").param("limit", "1").param("sort", "title")))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.page").value(2),
                        jsonPath("$.data.length()").value(1),
                        jsonPath("$.hasNext").value(false));
    }

    @Test
    void rejectsOutOfRangePaging() throws Exception {
        mockMvc.perform(as(tokenA, get("/api/library").param("limit", "0")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(tokenA, get("/api/library").param("limit", "51")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(tokenA, get("/api/library").param("page", "0")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(tokenA, get("/api/library").param("sort", "xyz")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hidesAnotherUsersSongBehindA404() throws Exception {
        String id = createSong(tokenA, "Riêng của A");

        mockMvc.perform(as(tokenB, get("/api/library/" + id)))
                .andExpectAll(status().isNotFound(), jsonPath("$.code").value("SONG_NOT_FOUND"));
        mockMvc.perform(as(tokenB, put("/api/library/" + id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SONG_BODY))
                .andExpect(status().isNotFound());
        mockMvc.perform(as(tokenB, delete("/api/library/" + id)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAStaleVersionWithAConflict() throws Exception {
        String id = createSong(tokenA, "Bản gốc");

        mockMvc.perform(as(tokenA, put("/api/library/" + id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Bản sửa 1","content":"C","originalKey":"C","targetKey":"C","version":0}
                                """))
                .andExpectAll(status().isOk(), jsonPath("$.version").value(1));

        mockMvc.perform(as(tokenA, put("/api/library/" + id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Bản sửa 2","content":"C","originalKey":"C","targetKey":"C","version":0}
                                """))
                .andExpectAll(status().isConflict(), jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    @Test
    void softDeletesTheSong() throws Exception {
        String id = createSong(tokenA, "Sắp xoá");

        mockMvc.perform(as(tokenA, delete("/api/library/" + id))).andExpect(status().isNoContent());
        mockMvc.perform(as(tokenA, get("/api/library/" + id))).andExpect(status().isNotFound());
        mockMvc.perform(as(tokenA, get("/api/library")))
                .andExpect(jsonPath("$.total").value(0));
        assertThat(songs.findById(UUID.fromString(id)))
                .get()
                .satisfies(song -> assertThat(song.getDeletedAt()).isNotNull());
    }

    @Test
    void refusesEveryEndpointWithoutAToken() throws Exception {
        String id = createSong(tokenA, "Bài có sẵn");

        mockMvc.perform(post("/api/library").contentType(MediaType.APPLICATION_JSON).content(SONG_BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/library")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/library/" + id)).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/library/" + id).contentType(MediaType.APPLICATION_JSON).content(SONG_BODY))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/library/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsANonUuidPathId() throws Exception {
        mockMvc.perform(as(tokenA, get("/api/library/not-a-uuid")))
                .andExpectAll(status().isBadRequest(), jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    // Creates a song owned by the given token's user and returns its id.
    private String createSong(String token, String title) throws Exception {
        String body = mockMvc.perform(as(token, post("/api/library"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","content":"C G","originalKey":"C","targetKey":"G"}
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String requestWithContent(String content) {
        return """
                {"title":"X","content":"%s","originalKey":"C","targetKey":"C"}
                """.formatted(content);
    }

    // Registers a new user and returns its access token.
    private String register(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Khoi PG","email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private MockHttpServletRequestBuilder as(String token, MockHttpServletRequestBuilder builder) {
        return builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}
