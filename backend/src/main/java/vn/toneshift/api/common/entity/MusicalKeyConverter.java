package vn.toneshift.api.common.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import vn.toneshift.api.common.enums.MusicalKey;

/**
 * Stores a {@link MusicalKey} as its sharp-spelled label.
 *
 * <p>Applied automatically to every {@code MusicalKey} attribute, because the alternative —
 * {@code @Enumerated(STRING)} — would write the constant name {@code "CS"} and violate the
 * {@code ck_song_keys} check constraint at runtime rather than at compile time.
 */
@Converter(autoApply = true)
public class MusicalKeyConverter implements AttributeConverter<MusicalKey, String> {

    @Override
    public String convertToDatabaseColumn(MusicalKey attribute) {
        return attribute == null ? null : attribute.label();
    }

    @Override
    public MusicalKey convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MusicalKey.from(dbData);
    }
}
