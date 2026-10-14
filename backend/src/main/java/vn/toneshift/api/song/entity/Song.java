package vn.toneshift.api.song.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.toneshift.api.common.entity.Auditable;
import vn.toneshift.api.common.entity.MusicalKeyConverter;
import vn.toneshift.api.common.enums.MusicalKey;
import vn.toneshift.api.common.enums.Visibility;
import vn.toneshift.api.user.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A chord sheet saved by a user. Private by default; publishing moves it to the community.
 *
 * <p>Deletion is soft, through {@link #deletedAt}. There is deliberately no
 * {@code @SQLRestriction}: every query filters {@code deletedAt is null} explicitly, so the
 * owner's own hard-delete paths and cleanup jobs can still see the rows.
 */
@Entity
@Table(name = "songs")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Song extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "artist", length = 200)
    private String artist;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    // The converter is auto-applied; naming it here keeps the storage form visible at the field.
    @Convert(converter = MusicalKeyConverter.class)
    @Column(name = "original_key", nullable = false, length = 2)
    private MusicalKey originalKey;

    @Convert(converter = MusicalKeyConverter.class)
    @Column(name = "target_key", nullable = false, length = 2)
    private MusicalKey targetKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 10)
    private Visibility visibility;

    @Column(name = "slug", length = 230)
    private String slug;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAvg;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /**
     * Creates a private song owned by the given user.
     *
     * <p>The counters and {@code visibility} are set here rather than left to the column defaults
     * because Hibernate issues a full INSERT and would otherwise send NULL.
     *
     * @param owner       the user saving the song
     * @param title       the song title
     * @param artist      the performer, may be null
     * @param content     the chord sheet
     * @param originalKey the key the sheet is written in
     * @param targetKey   the key it was transposed to
     */
    public Song(User owner, String title, String artist, String content,
                MusicalKey originalKey, MusicalKey targetKey) {
        this.owner = owner;
        this.title = title;
        this.artist = artist;
        this.content = content;
        this.originalKey = originalKey;
        this.targetKey = targetKey;
        this.visibility = Visibility.PRIVATE;
        // Scale 2 so the POST response and a later GET serialise identically as 0.00.
        this.ratingAvg = BigDecimal.ZERO.setScale(2);
        this.ratingCount = 0;
        this.commentCount = 0;
    }
}
