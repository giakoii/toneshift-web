package vn.toneshift.api.song.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.toneshift.api.common.dto.PageResponse;
import vn.toneshift.api.common.web.CurrentUser;
import vn.toneshift.api.song.dto.SongRequest;
import vn.toneshift.api.song.dto.SongResponse;
import vn.toneshift.api.song.dto.SongSummary;
import vn.toneshift.api.song.service.SongService;

import java.net.URI;
import java.util.UUID;

/**
 * Endpoints for the signed-in user's own library of saved chord sheets.
 *
 * <p>{@code @Validated} is required, not decorative: without it Spring MVC validates the parameter
 * constraints itself and raises an exception the global handler does not recognise, turning a bad
 * {@code limit} into a 500.
 */
@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class LibraryController {

    private final SongService songService;

    /**
     * Saves a new song to the caller's library.
     *
     * @param userId  the id of the signed-in user
     * @param request the song to save
     * @return the saved song, with its location
     */
    @PostMapping
    @Operation(summary = "Lưu bản cảm âm vào thư viện", description = "Bài mới luôn ở trạng thái PRIVATE.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bài hát đã lưu"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED hoặc MALFORMED_REQUEST"),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
    })
    public ResponseEntity<SongResponse> create(@CurrentUser UUID userId,
                                               @Valid @RequestBody SongRequest request) {
        SongResponse created = songService.create(userId, request);
        return ResponseEntity.created(URI.create("/api/library/" + created.id())).body(created);
    }

    /**
     * Lists the caller's songs.
     *
     * @param userId the id of the signed-in user
     * @param q      a search term matched against title and artist
     * @param page   the 1-based page number
     * @param limit  the page size
     * @param sort   {@code updated} or {@code title}
     * @return the matching page of songs
     */
    @GetMapping
    @Operation(summary = "Danh sách bài hát trong thư viện",
            description = "q tìm theo tên bài và nghệ sĩ. sort nhận 'updated' (mặc định) hoặc 'title'.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trang kết quả"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED"),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
    })
    public PageResponse<SongSummary> list(
            @CurrentUser UUID userId,
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
            @RequestParam(defaultValue = "updated") @Pattern(regexp = "updated|title") String sort) {
        return songService.listMine(userId, q, sort, page, limit);
    }

    /**
     * Returns one of the caller's songs with its full chord sheet.
     *
     * @param userId the id of the signed-in user
     * @param id     the song id
     * @return the song
     */
    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết một bài trong thư viện",
            description = "Trả 404 cả khi bài thuộc người khác, để không tiết lộ bài đó tồn tại.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nội dung bài hát"),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED"),
            @ApiResponse(responseCode = "404", description = "SONG_NOT_FOUND")
    })
    public SongResponse get(@CurrentUser UUID userId, @PathVariable UUID id) {
        return songService.getMine(userId, id);
    }

    /**
     * Replaces the contents of one of the caller's songs.
     *
     * @param userId  the id of the signed-in user
     * @param id      the song id
     * @param request the new values, optionally carrying the version last seen by the client
     * @return the updated song
     */
    @PutMapping("/{id}")
    @Operation(summary = "Sửa một bài trong thư viện",
            description = "Gửi version đọc được từ lần tải trước; nếu bài đã bị sửa ở nơi khác sẽ nhận 409.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bài hát sau khi sửa"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED hoặc MALFORMED_REQUEST"),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED"),
            @ApiResponse(responseCode = "404", description = "SONG_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "VERSION_CONFLICT")
    })
    public SongResponse update(@CurrentUser UUID userId,
                               @PathVariable UUID id,
                               @Valid @RequestBody SongRequest request) {
        return songService.update(userId, id, request);
    }

    /**
     * Removes one of the caller's songs from the library.
     *
     * @param userId the id of the signed-in user
     * @param id     the song id
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xoá một bài khỏi thư viện", description = "Xoá mềm; bài sẽ biến mất khỏi mọi nơi.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Đã xoá"),
            @ApiResponse(responseCode = "401", description = "UNAUTHORIZED"),
            @ApiResponse(responseCode = "404", description = "SONG_NOT_FOUND")
    })
    public void delete(@CurrentUser UUID userId, @PathVariable UUID id) {
        songService.delete(userId, id);
    }
}
