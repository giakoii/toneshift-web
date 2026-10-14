package vn.toneshift.api.song.dto;

import vn.toneshift.api.common.enums.MusicalKey;
import vn.toneshift.api.common.enums.Visibility;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One row of a song list query, flattened so the owner arrives on the same row as the song.
 *
 * <p>Not an API type — {@code SongMapper} turns it into {@link SongSummary}. It exists so the
 * SELECT list can be narrowed to an excerpt of {@code content} instead of loading whole entities.
 *
 * @param id           song identifier
 * @param slug         the public URL slug, null while private
 * @param title        the song title
 * @param artist       the performer, may be null
 * @param excerpt      the first 200 characters of the chord sheet
 * @param originalKey  the key the sheet is written in
 * @param targetKey    the key it was transposed to
 * @param visibility   whether the song is published
 * @param ratingAvg    the average community rating
 * @param ratingCount  how many ratings it has
 * @param commentCount how many comments it has
 * @param ownerId      the owner's identifier
 * @param ownerName    the owner's display name
 * @param ownerImage   the owner's avatar URL, may be null
 * @param publishedAt  when it was published, null while private
 * @param updatedAt    when it was last changed
 */
public record SongSummaryRow(
        UUID id,
        String slug,
        String title,
        String artist,
        String excerpt,
        MusicalKey originalKey,
        MusicalKey targetKey,
        Visibility visibility,
        BigDecimal ratingAvg,
        int ratingCount,
        int commentCount,
        UUID ownerId,
        String ownerName,
        String ownerImage,
        Instant publishedAt,
        Instant updatedAt) {
}
