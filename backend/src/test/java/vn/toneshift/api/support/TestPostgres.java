package vn.toneshift.api.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * The single PostgreSQL 16 container shared by every test in the JVM.
 *
 * <p>Started once on first use and left running until the JVM exits, where Testcontainers' own
 * reaper removes it. Starting a container per test class would add several seconds per class.
 */
public final class TestPostgres {

    private static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("toneshift_test");

    static {
        INSTANCE.start();
    }

    // Holder class, not instantiable.
    private TestPostgres() {
    }

    /**
     * Points the Spring datasource at the shared container.
     *
     * @param registry the registry used to contribute dynamic properties
     */
    public static void registerTo(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", INSTANCE::getUsername);
        registry.add("spring.datasource.password", INSTANCE::getPassword);
    }
}
