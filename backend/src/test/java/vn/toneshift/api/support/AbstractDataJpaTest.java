package vn.toneshift.api.support;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import vn.toneshift.api.config.JpaAuditingConfig;

/**
 * Base class for repository tests. Runs against the shared PostgreSQL container with the real
 * Flyway schema, because the queries rely on PostgreSQL features such as {@code citext} and
 * partial indexes that an in-memory database does not have.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// The @DataJpaTest slice only scans entities and repositories, so auditing must be imported
// explicitly or created_at/updated_at stay null and every insert fails the NOT NULL constraint.
@Import(JpaAuditingConfig.class)
public abstract class AbstractDataJpaTest {

    /**
     * Points the Spring datasource at the shared PostgreSQL test container.
     *
     * @param registry the registry used to contribute dynamic properties
     */
    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        TestPostgres.registerTo(registry);
    }
}
