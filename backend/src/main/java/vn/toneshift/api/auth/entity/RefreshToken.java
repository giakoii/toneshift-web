package vn.toneshift.api.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.toneshift.api.user.entity.User;

import java.time.Instant;
import java.util.UUID;

/**
 * One issued refresh token, stored only as a SHA-256 hash.
 *
 * <p>Tokens of one session share a {@code familyId}: rotation issues a new member of the same
 * family, so reusing a revoked member means the whole family is suspect.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // char(64), not varchar: ddl-auto=validate compares JDBC type codes, so CHAR must be explicit.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Id of the token that replaced this one on rotation; not a foreign key. */
    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Creates a refresh token record for an issued token.
     *
     * @param user      the owner
     * @param tokenHash the SHA-256 hex hash of the raw token
     * @param familyId  the session family this token belongs to
     * @param expiresAt the expiry instant
     * @param userAgent the client user agent, may be {@code null}
     */
    public RefreshToken(User user, String tokenHash, UUID familyId, Instant expiresAt, String userAgent) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
        this.userAgent = userAgent;
    }

    /**
     * Returns whether this token has been revoked.
     *
     * @return {@code true} if a revocation instant is set
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Returns whether this token is expired at the given instant.
     *
     * @param now the instant to compare against
     * @return {@code true} if the token is no longer valid
     */
    public boolean isExpiredAt(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
