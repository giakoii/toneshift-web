package vn.toneshift.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA auditing so created/updated timestamps are populated automatically.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
