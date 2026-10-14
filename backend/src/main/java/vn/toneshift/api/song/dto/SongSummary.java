package vn.toneshift.api.song.dto;

import vn.toneshift.api.common.dto.UserRef;
import vn.toneshift.api.common.enums.MusicalKey;
import vn.toneshift.api.common.enums.Visibility;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A song as it appears in a list. Carries an {@code excerpt} instead of the full chord sheet so a
 * page of results stays small.
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
 * @param owner        the user who saved it
 * @param publishedAt  when it was published, null while private
 * @param updatedAt    when it was last changed
 */
public record SongSummary(
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
        UserRef owner,
        Instant publishedAt,
        Instant updatedAt) {
}
