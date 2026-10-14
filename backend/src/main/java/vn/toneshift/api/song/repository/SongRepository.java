package vn.toneshift.api.song.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.toneshift.api.song.dto.SongSummaryRow;
import vn.toneshift.api.song.entity.Song;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link Song}. Soft-deleted rows are excluded by each query rather than by a
 * global filter.
 */
public interface SongRepository extends JpaRepository<Song, UUID>, JpaSpecificationExecutor<Song> {

    /**
     * Finds a live song owned by the given user.
     *
     * <p>Conflating "missing", "soft-deleted" and "someone else's" into one empty result is what
     * lets the service answer 404 for another user's song instead of leaking its existence.
     *
     * @param id      the song id
     * @param ownerId the id of the user who must own it
     * @return the matching song, if any
     */
    Optional<Song> findByIdAndOwnerIdAndDeletedAtIsNull(UUID id, UUID ownerId);

    /**
     * Lists a user's live songs, narrowed to the fields a list view needs.
     *
     * <p>Selects only an excerpt of {@code content}, and joins the owner on the same row, so a page
     * costs two statements and never transfers whole chord sheets.
     *
     * @param ownerId  the id of the user whose songs to list
     * @param pattern  an ILIKE pattern matched against title and artist; {@code %} matches all
     * @param pageable the page, size and sort to apply
     * @return the matching rows
     */
    @Query(value = """
            select new vn.toneshift.api.song.dto.SongSummaryRow(
                s.id, s.slug, s.title, s.artist, substring(s.content, 1, 200),
                s.originalKey, s.targetKey, s.visibility,
                s.ratingAvg, s.ratingCount, s.commentCount,
                o.id, o.name, o.imageUrl,
                s.publishedAt, s.updatedAt)
            from Song s
              join s.owner o
            where o.id = :ownerId
              and s.deletedAt is null
              and (s.title ilike :pattern escape '!' or s.artist ilike :pattern escape '!')
            """,
            countQuery = """
            select count(s)
            from Song s
            where s.owner.id = :ownerId
              and s.deletedAt is null
              and (s.title ilike :pattern escape '!' or s.artist ilike :pattern escape '!')
            """)
    Page<SongSummaryRow> findMine(@Param("ownerId") UUID ownerId,
                                  @Param("pattern") String pattern,
                                  Pageable pageable);
}
