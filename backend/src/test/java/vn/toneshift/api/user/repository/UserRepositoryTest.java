package vn.toneshift.api.user.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import vn.toneshift.api.common.enums.Role;
import vn.toneshift.api.common.enums.UserStatus;
import vn.toneshift.api.support.AbstractDataJpaTest;
import vn.toneshift.api.user.entity.User;

import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRepositoryTest extends AbstractDataJpaTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findsAUserByItsStoredEmail() {
        String email = uniqueEmail();
        users.saveAndFlush(user(email));
        entityManager.clear();

        assertThat(users.findByEmail(email)).isPresent();
        assertThat(users.findByEmail(uniqueEmail())).isEmpty();
    }

    /**
     * Documents a trap: although the column is {@code citext}, the driver binds parameters as
     * {@code varchar}, so PostgreSQL compares them as {@code text} and the lookup is
     * case-sensitive. Callers must lowercase the email first; case-insensitive <em>uniqueness</em>
     * still holds, because the index compares stored citext values.
     */
    @Test
    void lookupsAreCaseSensitiveSoCallersMustNormaliseFirst() {
        String email = uniqueEmail();
        users.saveAndFlush(user(email));
        entityManager.clear();

        assertThat(users.findByEmail(email.toUpperCase(Locale.ROOT))).isEmpty();
        assertThat(users.existsByEmail(email.toUpperCase(Locale.ROOT))).isFalse();
        assertThat(users.existsByEmail(email)).isTrue();
    }

    @Test
    void rejectsASecondUserWithTheSameEmailRegardlessOfCase() {
        String email = uniqueEmail();
        users.saveAndFlush(user(email));

        assertThatThrownBy(() -> users.saveAndFlush(user(email.toUpperCase(Locale.ROOT))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void fillsAuditTimestampsOnSave() {
        User saved = users.saveAndFlush(user(uniqueEmail()));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void findDetailByIdLoadsProviderLinks() {
        User saved = users.saveAndFlush(user(uniqueEmail()));
        entityManager.clear();

        assertThat(users.findDetailById(saved.getId()))
                .get()
                .satisfies(found -> assertThat(found.getProviders()).isEmpty());
    }

    // Builds a credentials user with the given email.
    private User user(String email) {
        User user = new User(email, "Khoi PG", Role.USER, UserStatus.ACTIVE);
        user.setPasswordHash("$2a$12$abcdefghijklmnopqrstuv");
        return user;
    }

    // Emails are unique per call so these tests do not depend on an empty database.
    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
