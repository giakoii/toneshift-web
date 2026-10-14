package vn.toneshift.api.song.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.toneshift.api.common.dto.PageResponse;
import vn.toneshift.api.common.exception.ConflictException;
import vn.toneshift.api.common.exception.ErrorCode;
import vn.toneshift.api.common.exception.NotFoundException;
import vn.toneshift.api.song.dto.SongRequest;
import vn.toneshift.api.song.dto.SongResponse;
import vn.toneshift.api.song.dto.SongSummary;
import vn.toneshift.api.song.dto.SongSummaryRow;
import vn.toneshift.api.song.entity.Song;
import vn.toneshift.api.song.mapper.SongMapper;
import vn.toneshift.api.song.repository.SongRepository;
import vn.toneshift.api.user.entity.User;
import vn.toneshift.api.user.repository.UserRepository;

import java.text.Normalizer;
import java.time.Instant;
import java.util.UUID;

/**
 * Default {@link SongService}.
 */
@Service
@RequiredArgsConstructor
public class SongServiceImpl implements SongService {

    private static final String SORT_BY_TITLE = "title";

    private final SongRepository songs;
    private final UserRepository users;
    private final SongMapper songMapper;

    @Override
    @Transactional
    public SongResponse create(UUID userId, SongRequest request) {
        User owner = users.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
        Song song = new Song(
                owner,
                request.title().trim(),
                blankToNull(request.artist()),
                sanitizeContent(request.content()),
                request.originalKey(),
                request.targetKey());
        return songMapper.toResponse(songs.save(song));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SongSummary> listMine(UUID userId, String q, String sort, int page, int limit) {
        Page<SongSummaryRow> rows = songs.findMine(userId, likePattern(q), pageable(sort, page, limit));
        return PageResponse.of(rows.map(songMapper::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public SongResponse getMine(UUID userId, UUID id) {
        return songMapper.toResponse(requireMine(userId, id));
    }

    @Override
    @Transactional
    public SongResponse update(UUID userId, UUID id, SongRequest request) {
        Song song = requireMine(userId, id);
        // The entity is loaded fresh, so Hibernate's own @Version check would compare against the
        // current row and pass. Comparing the client's version here is what catches a stale editor.
        if (request.version() != null && request.version() != song.getVersion()) {
            throw new ConflictException(ErrorCode.VERSION_CONFLICT);
        }
        song.setTitle(request.title().trim());
        song.setArtist(blankToNull(request.artist()));
        song.setContent(sanitizeContent(request.content()));
        song.setOriginalKey(request.originalKey());
        song.setTargetKey(request.targetKey());
        return songMapper.toResponse(song);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID id) {
        requireMine(userId, id).setDeletedAt(Instant.now());
    }

    // Loads a live song owned by the caller, reporting anything else as missing.
    private Song requireMine(UUID userId, UUID id) {
        return songs.findByIdAndOwnerIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SONG_NOT_FOUND));
    }

    // Builds the page request, adding an id tie-break so paging stays stable between requests.
    private Pageable pageable(String sort, int page, int limit) {
        Sort order = SORT_BY_TITLE.equals(sort)
                ? Sort.by(Sort.Order.asc(SORT_BY_TITLE), Sort.Order.asc("id"))
                : Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"));
        return PageRequest.of(page - 1, limit, order);
    }

    // Turns a search term into an ILIKE pattern. "No filter" is the pattern '%' rather than a null
    // parameter, so the query has one shape and Postgres never sees an untyped NULL binding.
    private String likePattern(String q) {
        if (q == null || q.isBlank()) {
            return "%";
        }
        String escaped = q.trim()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    // Normalises to NFC so that cutting an excerpt at 200 characters cannot split a Vietnamese
    // grapheme, and strips NUL, which Postgres rejects in a text column. Newlines are kept: they
    // are what makes a chord sheet readable.
    private String sanitizeContent(String content) {
        return Normalizer.normalize(content, Normalizer.Form.NFC).replace("\u0000", "");
    }

    // Collapses a blank artist to null so "" and null do not produce two different-looking rows.
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
