package vn.toneshift.api.auth.entity;

import jakarta.persistence.Column;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import vn.toneshift.api.common.enums.AuthProviderType;
import vn.toneshift.api.user.entity.User;

import java.time.Instant;
import java.util.UUID;

/**
 * Link between a user and an external identity, keyed by the provider and its subject id.
 */
@Entity
@Table(name = "auth_providers")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private AuthProviderType provider;

    /** The provider's own subject identifier, such as Google's {@code sub} claim. */
    @Column(name = "provider_account_id", nullable = false, length = 100)
    private String providerAccountId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Creates an identity link.
     *
     * @param user              the user the identity belongs to
     * @param provider          the external provider
     * @param providerAccountId the subject id issued by that provider
     */
    public AuthProvider(User user, AuthProviderType provider, String providerAccountId) {
        this.user = user;
        this.provider = provider;
        this.providerAccountId = providerAccountId;
    }
}
