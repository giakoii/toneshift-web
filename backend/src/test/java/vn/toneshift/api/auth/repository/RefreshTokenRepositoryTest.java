package vn.toneshift.api.auth.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import vn.toneshift.api.auth.entity.RefreshToken;
import vn.toneshift.api.common.enums.Role;
import vn.toneshift.api.common.enums.UserStatus;
import vn.toneshift.api.common.util.HashUtils;
import vn.toneshift.api.support.AbstractDataJpaTest;
import vn.toneshift.api.user.entity.User;
import vn.toneshift.api.user.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenRepositoryTest extends AbstractDataJpaTest {

    @Autowired
    private RefreshTokenRepository tokens;

    @Autowired
    private UserRepository users;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findsATokenByItsHash() {
        User user = users.saveAndFlush(user(uniqueEmail()));
        String raw = HashUtils.randomToken();
        tokens.saveAndFlush(token(user, raw, UUID.randomUUID()));
        entityManager.clear();

        assertThat(tokens.findByTokenHash(HashUtils.sha256Hex(raw))).isPresent();
        assertThat(tokens.findByTokenHash(HashUtils.sha256Hex("other"))).isEmpty();
    }

    @Test
    void revokeFamilyOnlyTouchesActiveTokensOfThatFamily() {
        User user = users.saveAndFlush(user(uniqueEmail()));
        UUID family = UUID.randomUUID();
        UUID otherFamily = UUID.randomUUID();
        tokens.saveAndFlush(token(user, HashUtils.randomToken(), family));
        tokens.saveAndFlush(token(user, HashUtils.randomToken(), family));
        tokens.saveAndFlush(token(user, HashUtils.randomToken(), otherFamily));

        int revoked = tokens.revokeFamily(family, Instant.now());

        assertThat(revoked).isEqualTo(2);
        // Revoking again is a no-op, which is what makes logout idempotent.
        assertThat(tokens.revokeFamily(family, Instant.now())).isZero();
    }

    @Test
    void revokeAllByUserOnlyTouchesThatUsersActiveTokens() {
        User user = users.saveAndFlush(user(uniqueEmail()));
        User other = users.saveAndFlush(user(uniqueEmail()));
        tokens.saveAndFlush(token(user, HashUtils.randomToken(), UUID.randomUUID()));
        tokens.saveAndFlush(token(user, HashUtils.randomToken(), UUID.randomUUID()));
        tokens.saveAndFlush(token(other, HashUtils.randomToken(), UUID.randomUUID()));

        int revoked = tokens.revokeAllByUser(user.getId(), Instant.now());

        assertThat(revoked).isEqualTo(2);
        assertThat(tokens.revokeAllByUser(other.getId(), Instant.now())).isEqualTo(1);
    }

    @Test
    void rejectsTwoTokensWithTheSameHash() {
        User user = users.saveAndFlush(user(uniqueEmail()));
        String raw = HashUtils.randomToken();
        tokens.saveAndFlush(token(user, raw, UUID.randomUUID()));

        assertThatThrownBy(() -> tokens.saveAndFlush(token(user, raw, UUID.randomUUID())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Builds a credentials user with the given email.
    private User user(String email) {
        return new User(email, "Khoi PG", Role.USER, UserStatus.ACTIVE);
    }

    // Emails are unique per call so these tests do not depend on an empty database.
    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    // Builds an active refresh token expiring in 30 days.
    private RefreshToken token(User user, String rawToken, UUID familyId) {
        return new RefreshToken(
                user,
                HashUtils.sha256Hex(rawToken),
                familyId,
                Instant.now().plus(30, ChronoUnit.DAYS),
                "JUnit");
    }
}
