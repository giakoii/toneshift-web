package vn.toneshift.api.song.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.toneshift.api.common.enums.MusicalKey;

/**
 * A chord sheet to save or update.
 *
 * @param title       the song title
 * @param artist      the performer, may be null
 * @param content     the chord sheet as typed by the user, before transposing
 * @param originalKey the key the sheet is written in
 * @param targetKey   the key it was transposed to
 * @param version     the version the client last saw; sent on update only, and a mismatch is
 *                    rejected with {@code 409 VERSION_CONFLICT}
 */
public record SongRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 200) String artist,
        @NotBlank @Size(max = 50000) String content,
        @NotNull MusicalKey originalKey,
        @NotNull MusicalKey targetKey,
        Long version) {
}
