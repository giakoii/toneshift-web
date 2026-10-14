package vn.toneshift.api.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.toneshift.api.auth.entity.AuthProvider;
import vn.toneshift.api.common.enums.AuthProviderType;

import java.util.Optional;
import java.util.UUID;

/**
 * Data access for {@link AuthProvider} identity links.
 */
public interface AuthProviderRepository extends JpaRepository<AuthProvider, UUID> {

    /**
     * Finds the identity link for a provider subject, which identifies the account to sign in.
     *
     * @param provider          the external provider
     * @param providerAccountId the subject id issued by that provider
     * @return the matching link, if any
     */
    Optional<AuthProvider> findByProviderAndProviderAccountId(AuthProviderType provider, String providerAccountId);

    /**
     * Finds the link a user already has for a provider, to detect a changed subject id.
     *
     * @param userId   the user id
     * @param provider the external provider
     * @return the matching link, if any
     */
    Optional<AuthProvider> findByUserIdAndProvider(UUID userId, AuthProviderType provider);
}
