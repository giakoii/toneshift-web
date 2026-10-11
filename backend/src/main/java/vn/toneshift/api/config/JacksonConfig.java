package vn.toneshift.api.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Configures JSON serialization: ISO-8601 UTC dates and null fields kept in responses.
 */
@Configuration
public class JacksonConfig {

    /**
     * Customizes the Jackson ObjectMapper builder with the project's JSON conventions.
     *
     * @return the builder customizer
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .serializationInclusion(JsonInclude.Include.ALWAYS)
                .timeZone(TimeZone.getTimeZone("UTC"));
    }
}
