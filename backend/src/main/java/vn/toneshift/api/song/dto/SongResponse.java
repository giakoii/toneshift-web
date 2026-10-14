package vn.toneshift.api.song.dto;

import vn.toneshift.api.common.dto.UserRef;
import vn.toneshift.api.common.enums.MusicalKey;
import vn.toneshift.api.common.enums.Visibility;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A single song with its full chord sheet.
 *
 * @param id          song identifier
 * @param slug        the public URL slug, null while private
 * @param title       the song title
 * @param artist      the performer, may be null
 * @param content     the chord sheet
 * @param originalKey the key the sheet is written in
 * @param targetKey   the key it was transposed to
 * @param visibility  whether the song is published
 * @param publishedAt when it was published, null while private
 * @param ratingAvg   the average community rating
 * @param ratingCount how many ratings it has
 * @param owner       the user who saved it
 * @param version     the current version, to send back on the next update
 * @param createdAt   when it was saved
 * @param updatedAt   when it was last changed
 * @param myScore     the viewer's own rating; always null on the library endpoints
 */
public record SongResponse(
        UUID id,
        String slug,
        String title,
        String artist,
        String content,
        MusicalKey originalKey,
        MusicalKey targetKey,
        Visibility visibility,
        Instant publishedAt,
        BigDecimal ratingAvg,
        int ratingCount,
        UserRef owner,
        long version,
        Instant createdAt,
        Instant updatedAt,
        Short myScore) {
}
