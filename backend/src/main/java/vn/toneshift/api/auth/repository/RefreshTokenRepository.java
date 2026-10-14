package vn.toneshift.api.auth.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.toneshift.api.auth.entity.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link RefreshToken}. Tokens are looked up by their SHA-256 hash, never by raw value.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Finds a token by the hash of its raw value.
     *
     * @param tokenHash the SHA-256 hex hash
     * @return the matching token, if any
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Finds a token and locks its row, so two requests rotating the same token are serialised and
     * only one of them can succeed.
     *
     * <p>Deliberately does not fetch the user: Hibernate emits a bare {@code for update}, which in
     * PostgreSQL would lock the joined {@code users} row as well and block unrelated profile
     * writes. The user is loaded through its lazy proxy inside the transaction instead.
     *
     * @param tokenHash the SHA-256 hex hash
     * @return the matching token, if any
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    /**
     * Revokes every token of a session family that is still active.
     *
     * @param familyId the family to revoke
     * @param now      the revocation instant
     * @return the number of tokens revoked
     */
    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);

    /**
     * Revokes every active token of a user, signing them out everywhere.
     *
     * @param userId the user whose tokens to revoke
     * @param now    the revocation instant
     * @return the number of tokens revoked
     */
    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.user.id = :userId and t.revokedAt is null")
    int revokeAllByUser(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Deletes tokens that expired before the cutoff.
     *
     * <p>Expired tokens are kept for a while rather than deleted immediately, so that a replay of
     * a recently leaked token is still recognised as reuse instead of looking like an unknown token.
     *
     * @param cutoff the expiry instant before which tokens are removed
     * @return the number of tokens deleted
     */
    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
