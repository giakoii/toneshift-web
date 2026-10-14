package vn.toneshift.api.auth.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import vn.toneshift.api.auth.entity.AuthProvider;
import vn.toneshift.api.common.enums.AuthProviderType;
import vn.toneshift.api.common.enums.Role;
import vn.toneshift.api.common.enums.UserStatus;
import vn.toneshift.api.support.AbstractDataJpaTest;
import vn.toneshift.api.user.entity.User;
import vn.toneshift.api.user.repository.UserRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthProviderRepositoryTest extends AbstractDataJpaTest {

    @Autowired
    private AuthProviderRepository links;

    @Autowired
    private UserRepository users;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findsTheLinkByProviderSubject() {
        User user = users.saveAndFlush(user(uniqueEmail()));
        links.saveAndFlush(new AuthProvider(user, AuthProviderType.GOOGLE, "google-sub-1"));
        entityManager.clear();

        assertThat(links.findByProviderAndProviderAccountId(AuthProviderType.GOOGLE, "google-sub-1"))
                .get()
                .satisfies(link -> assertThat(link.getUser().getId()).isEqualTo(user.getId()));
        assertThat(links.findByProviderAndProviderAccountId(AuthProviderType.GOOGLE, "unknown")).isEmpty();
    }

    @Test
    void findsTheLinkAUserAlreadyHasForAProvider() {
        User user = users.saveAndFlush(user(uniqueEmail()));
        links.saveAndFlush(new AuthProvider(user, AuthProviderType.GOOGLE, "google-sub-1"));
        entityManager.clear();

        assertThat(links.findByUserIdAndProvider(user.getId(), AuthProviderType.GOOGLE))
                .get()
                .satisfies(link -> assertThat(link.getProviderAccountId()).isEqualTo("google-sub-1"));
    }

    @Test
    void rejectsTheSameProviderSubjectLinkedToTwoUsers() {
        User first = users.saveAndFlush(user(uniqueEmail()));
        User second = users.saveAndFlush(user(uniqueEmail()));
        links.saveAndFlush(new AuthProvider(first, AuthProviderType.GOOGLE, "google-sub-1"));

        assertThatThrownBy(() -> links.saveAndFlush(new AuthProvider(second, AuthProviderType.GOOGLE, "google-sub-1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsTwoLinksOfTheSameProviderForOneUser() {
        User user = users.saveAndFlush(user(uniqueEmail()));
        links.saveAndFlush(new AuthProvider(user, AuthProviderType.GOOGLE, "google-sub-1"));

        assertThatThrownBy(() -> links.saveAndFlush(new AuthProvider(user, AuthProviderType.GOOGLE, "google-sub-2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Builds a provider-only user with the given email.
    private User user(String email) {
        return new User(email, "Khoi PG", Role.USER, UserStatus.ACTIVE);
    }

    // Emails are unique per call so these tests do not depend on an empty database.
    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
