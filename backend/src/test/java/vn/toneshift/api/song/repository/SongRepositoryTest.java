package vn.toneshift.api.song.repository;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import vn.toneshift.api.common.enums.MusicalKey;
import vn.toneshift.api.common.enums.Role;
import vn.toneshift.api.common.enums.UserStatus;
import vn.toneshift.api.song.dto.SongSummaryRow;
import vn.toneshift.api.song.entity.Song;
import vn.toneshift.api.support.AbstractDataJpaTest;
import vn.toneshift.api.user.entity.User;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Statistics are enabled only for this class so the rest of the suite is not slowed down by them.
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class SongRepositoryTest extends AbstractDataJpaTest {

    private static final String MATCH_ALL = "%";

    @Autowired
    private SongRepository songs;

    @Autowired
    private TestEntityManager entityManager;

    /**
     * Guards the one failure the schema validator cannot catch: {@code @Enumerated(STRING)} would
     * store the constant name {@code "CS"} and violate {@code ck_song_keys}, so the key must reach
     * the column as its sharp label.
     */
    @Test
    void storesTheSharpLabelRatherThanTheEnumConstantName() {
        User owner = persistOwner();
        Song saved = songs.saveAndFlush(song(owner, "Nơi này có anh", "Sơn Tùng M-TP", "C G", MusicalKey.CS, MusicalKey.DS));
        entityManager.clear();

        Object[] stored = (Object[]) entityManager.getEntityManager()
                .createNativeQuery("select original_key, target_key from songs where id = :id")
                .setParameter("id", saved.getId())
                .getSingleResult();

        assertThat(stored[0]).isEqualTo("C#");
        assertThat(stored[1]).isEqualTo("D#");
        assertThat(songs.findById(saved.getId()))
                .get()
                .satisfies(found -> {
                    assertThat(found.getOriginalKey()).isEqualTo(MusicalKey.CS);
                    assertThat(found.getTargetKey()).isEqualTo(MusicalKey.DS);
                });
    }

    @Test
    void appliesTheDefaultsThatTheColumnsRequire() {
        User owner = persistOwner();
        Song saved = songs.saveAndFlush(song(owner, "Bài mới", null, "C G", MusicalKey.C, MusicalKey.G));

        assertThat(saved.getVisibility()).isEqualTo(vn.toneshift.api.common.enums.Visibility.PRIVATE);
        assertThat(saved.getRatingAvg()).isEqualByComparingTo("0.00");
        assertThat(saved.getRatingCount()).isZero();
        assertThat(saved.getCommentCount()).isZero();
        assertThat(saved.getSlug()).isNull();
        assertThat(saved.getPublishedAt()).isNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void hidesAnotherOwnersAndSoftDeletedSongs() {
        User owner = persistOwner();
        User stranger = persistOwner();
        Song saved = songs.saveAndFlush(song(owner, "Riêng tư", null, "C", MusicalKey.C, MusicalKey.C));
        entityManager.clear();

        assertThat(songs.findByIdAndOwnerIdAndDeletedAtIsNull(saved.getId(), owner.getId())).isPresent();
        assertThat(songs.findByIdAndOwnerIdAndDeletedAtIsNull(saved.getId(), stranger.getId())).isEmpty();

        songs.findById(saved.getId()).orElseThrow().setDeletedAt(Instant.now());
        entityManager.flush();
        entityManager.clear();

        assertThat(songs.findByIdAndOwnerIdAndDeletedAtIsNull(saved.getId(), owner.getId())).isEmpty();
        // The row itself survives, so cleanup jobs and audits can still reach it.
        assertThat(songs.findById(saved.getId())).isPresent();
    }

    @Test
    void matchesTitleAndArtistCaseInsensitively() {
        User owner = persistOwner();
        songs.saveAndFlush(song(owner, "Nơi này có anh", "Sơn Tùng M-TP", "C", MusicalKey.C, MusicalKey.C));
        songs.saveAndFlush(song(owner, "Chúng ta của hiện tại", null, "C", MusicalKey.C, MusicalKey.C));
        entityManager.clear();

        assertThat(findMine(owner, "%NƠI%").getContent()).hasSize(1);
        assertThat(findMine(owner, "%tùng%").getContent()).hasSize(1);
        assertThat(findMine(owner, MATCH_ALL).getContent()).hasSize(2);
    }

    /**
     * A search term containing {@code %} must be treated as text. Without the {@code !} escape the
     * pattern would match every row.
     */
    @Test
    void escapesLikeWildcardsInTheSearchTerm() {
        User owner = persistOwner();
        songs.saveAndFlush(song(owner, "100% Love", null, "C", MusicalKey.C, MusicalKey.C));
        songs.saveAndFlush(song(owner, "Love", null, "C", MusicalKey.C, MusicalKey.C));
        entityManager.clear();

        assertThat(findMine(owner, "%100!%%").getContent())
                .extracting(SongSummaryRow::title)
                .containsExactly("100% Love");
    }

    @Test
    void cutsTheExcerptAtTwoHundredCharacters() {
        User owner = persistOwner();
        songs.saveAndFlush(song(owner, "Dài", null, "C".repeat(500), MusicalKey.C, MusicalKey.C));
        songs.saveAndFlush(song(owner, "Ngắn", null, "G".repeat(50), MusicalKey.C, MusicalKey.C));
        entityManager.clear();

        assertThat(findMine(owner, "%Dài%").getContent().getFirst().excerpt()).hasSize(200);
        assertThat(findMine(owner, "%Ngắn%").getContent().getFirst().excerpt()).hasSize(50);
    }

    @Test
    void sortsByTitleWhenAsked() {
        User owner = persistOwner();
        songs.saveAndFlush(song(owner, "Bêta", null, "C", MusicalKey.C, MusicalKey.C));
        songs.saveAndFlush(song(owner, "Alpha", null, "C", MusicalKey.C, MusicalKey.C));
        entityManager.clear();

        Page<SongSummaryRow> page = songs.findMine(owner.getId(), MATCH_ALL,
                PageRequest.of(0, 20, Sort.by(Sort.Order.asc("title"), Sort.Order.asc("id"))));

        assertThat(page.getContent()).extracting(SongSummaryRow::title).containsExactly("Alpha", "Bêta");
    }

    @Test
    void returnsAnEmptyPageBeyondTheLastOne() {
        User owner = persistOwner();
        songs.saveAndFlush(song(owner, "Một bài", null, "C", MusicalKey.C, MusicalKey.C));
        entityManager.clear();

        Page<SongSummaryRow> page = songs.findMine(owner.getId(), MATCH_ALL,
                PageRequest.of(998, 20, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    /**
     * The list must cost one query plus one count, no matter how many rows come back — and because
     * {@link SongSummaryRow} has no {@code content} field, whole chord sheets cannot leak into it.
     */
    @Test
    void listsAPageInExactlyTwoStatements() {
        User owner = persistOwner();
        for (int i = 0; i < 10; i++) {
            songs.saveAndFlush(song(owner, "Bài " + i, "Nghệ sĩ " + i, "C G Am F", MusicalKey.C, MusicalKey.G));
        }
        entityManager.flush();
        entityManager.clear();

        SessionFactory sessionFactory = entityManager.getEntityManager()
                .getEntityManagerFactory()
                .unwrap(SessionFactory.class);
        long before = sessionFactory.getStatistics().getPrepareStatementCount();

        Page<SongSummaryRow> page = findMine(owner, MATCH_ALL);

        assertThat(page.getContent()).hasSize(10);
        assertThat(sessionFactory.getStatistics().getPrepareStatementCount() - before).isEqualTo(2);
    }

    // Runs the library list query with the default sort.
    private Page<SongSummaryRow> findMine(User owner, String pattern) {
        return songs.findMine(owner.getId(), pattern,
                PageRequest.of(0, 20, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))));
    }

    // Persists a fresh owner so each test sees only its own songs.
    private User persistOwner() {
        User owner = new User("owner-" + UUID.randomUUID() + "@example.com", "Khoi PG", Role.USER, UserStatus.ACTIVE);
        return entityManager.persistAndFlush(owner);
    }

    private Song song(User owner, String title, String artist, String content,
                      MusicalKey originalKey, MusicalKey targetKey) {
        return new Song(owner, title, artist, content, originalKey, targetKey);
    }
}
