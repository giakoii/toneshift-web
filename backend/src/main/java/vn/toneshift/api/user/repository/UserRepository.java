package vn.toneshift.api.user.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.toneshift.api.user.entity.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link User}. Email lookups are case-insensitive because the column is {@code citext}.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by email, ignoring case.
     *
     * @param email the email to look up
     * @return the matching user, if any
     */
    Optional<User> findByEmail(String email);

    /**
     * Returns whether a user with the given email exists, ignoring case.
     *
     * @param email the email to check
     * @return {@code true} if the email is taken
     */
    boolean existsByEmail(String email);

    /**
     * Finds a user with its provider links already loaded, for responses that expose them.
     *
     * @param id the user id
     * @return the matching user with providers fetched, if any
     */
    @EntityGraph(attributePaths = "providers")
    Optional<User> findDetailById(UUID id);
}
