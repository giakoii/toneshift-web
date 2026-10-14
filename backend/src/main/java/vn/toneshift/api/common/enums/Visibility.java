package vn.toneshift.api.common.enums;

/**
 * Whether a song is only visible to its owner or published to the community.
 *
 * <p>Stored as a string in {@code songs.visibility}; the constant names match
 * {@code ck_song_visibility}, so {@code @Enumerated(STRING)} is safe here.
 */
public enum Visibility {

    PRIVATE,
    PUBLIC
}
