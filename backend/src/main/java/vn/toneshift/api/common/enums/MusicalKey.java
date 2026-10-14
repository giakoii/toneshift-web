package vn.toneshift.api.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * One of the twelve chromatic keys, always spelled with a sharp.
 *
 * <p>The Java constant names cannot be used on the wire or in the database: {@code CS.name()} is
 * {@code "CS"}, which violates {@code ck_song_keys}. Both directions go through {@link #label()},
 * and persistence is handled by {@code MusicalKeyConverter} rather than {@code @Enumerated}.
 */
public enum MusicalKey {

    C("C"),
    CS("C#"),
    D("D"),
    DS("D#"),
    E("E"),
    F("F"),
    FS("F#"),
    G("G"),
    GS("G#"),
    A("A"),
    AS("A#"),
    B("B");

    private final String label;

    // Binds the constant to the single spelling used by the API and the database.
    MusicalKey(String label) {
        this.label = label;
    }

    /**
     * Returns the wire and database form of this key, such as {@code "C#"}.
     *
     * @return the sharp-spelled label
     */
    @JsonValue
    public String label() {
        return label;
    }

    /**
     * Resolves a label to its key, case-sensitively.
     *
     * <p>Flat spellings such as {@code "Db"} and lowercase input are rejected on purpose: the
     * frontend normalises them before sending. The thrown exception reaches the client as
     * {@code 400 MALFORMED_REQUEST}.
     *
     * @param value the label to resolve
     * @return the matching key
     * @throws IllegalArgumentException if no key carries that label
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static MusicalKey from(String value) {
        for (MusicalKey key : values()) {
            if (key.label.equals(value)) {
                return key;
            }
        }
        throw new IllegalArgumentException("Tone không hợp lệ: " + value);
    }
}
