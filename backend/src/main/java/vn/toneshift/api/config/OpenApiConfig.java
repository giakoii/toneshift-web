package vn.toneshift.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the OpenAPI (Swagger) metadata and the JWT bearer security scheme.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    /**
     * Builds the OpenAPI definition with API info and bearer authentication.
     *
     * @return the OpenAPI definition
     */
    @Bean
    public OpenAPI toneShiftOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ToneShift API")
                        .version("v1")
                        .description("API cho ToneShift – chuyển đổi và chia sẻ cảm âm"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
