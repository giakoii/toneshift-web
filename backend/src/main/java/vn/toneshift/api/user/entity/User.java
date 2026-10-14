package vn.toneshift.api.user.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.toneshift.api.auth.entity.AuthProvider;
import vn.toneshift.api.common.entity.Auditable;
import vn.toneshift.api.common.enums.Role;
import vn.toneshift.api.common.enums.UserStatus;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A registered account. Credentials accounts carry a password hash; provider-only accounts have none.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // citext: the unique index is case-insensitive, so values are stored lowercase.
    @Column(name = "email", nullable = false, columnDefinition = "citext")
    private String email;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private UserStatus status;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AuthProvider> providers = new LinkedHashSet<>();

    /**
     * Creates a user with the fields every registration path sets; the rest are set through setters.
     *
     * @param email  the lowercase email
     * @param name   the display name
     * @param role   the role to grant
     * @param status the initial account status
     */
    public User(String email, String name, Role role, UserStatus status) {
        this.email = email;
        this.name = name;
        this.role = role;
        this.status = status;
    }

    /**
     * Links an external identity to this user, keeping both sides of the association in sync.
     *
     * @param provider the identity link to add
     */
    public void addProvider(AuthProvider provider) {
        providers.add(provider);
        provider.setUser(this);
    }

    /**
     * Returns whether this account can sign in with a password.
     *
     * @return {@code true} if a password hash is set
     */
    public boolean hasPassword() {
        return passwordHash != null;
    }

    /**
     * Returns whether this account is blocked.
     *
     * @return {@code true} if the status is {@link UserStatus#BLOCKED}
     */
    public boolean isBlocked() {
        return status == UserStatus.BLOCKED;
    }
}
