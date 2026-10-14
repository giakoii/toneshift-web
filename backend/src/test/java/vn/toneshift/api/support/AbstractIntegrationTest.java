package vn.toneshift.api.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for integration tests running against the real PostgreSQL 16 container
 * shared through {@link TestPostgres}.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

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
