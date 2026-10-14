package vn.toneshift.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import vn.toneshift.api.common.web.CurrentUserArgumentResolver;

import java.util.List;

/**
 * Registers the web MVC extensions used by controllers.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    /**
     * Adds the resolver backing {@code @CurrentUser} controller parameters.
     *
     * @param resolvers the resolver list to contribute to
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
