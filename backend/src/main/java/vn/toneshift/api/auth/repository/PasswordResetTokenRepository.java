package vn.toneshift.api.auth.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.toneshift.api.auth.entity.PasswordResetToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link PasswordResetToken}. Tokens are looked up by their SHA-256 hash.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Finds a reset token by the hash of its raw value.
     *
     * @param tokenHash the SHA-256 hex hash
     * @return the matching token, if any
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Finds a reset token and locks its row, so the same token cannot be redeemed twice
     * concurrently.
     *
     * @param tokenHash the SHA-256 hex hash
     * @return the matching token, if any
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetToken t where t.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    /**
     * Marks every outstanding token of a user as used, so requesting a new link invalidates the
     * previous ones instead of leaving several usable at once.
     *
     * @param userId the user whose tokens to retire
     * @param now    the instant to record
     * @return the number of tokens retired
     */
    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = :now where t.user.id = :userId and t.usedAt is null")
    int markOutstandingAsUsed(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Deletes tokens that can no longer be redeemed, whether used or expired.
     *
     * @param now the current instant
     * @return the number of tokens deleted
     */
    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :now or t.usedAt is not null")
    int deleteUnusable(@Param("now") Instant now);
}
