package codeping.flex.gateway.global.config;

import codeping.flex.gateway.global.properties.ServerDomainProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {
    private final ServerDomainProperties serverDomainProperties;

    @Bean
    @Profile("local")
    public CorsWebFilter localCorsWebFilter() {
        return createCorsWebFilter(getLocalAllowedOrigins());
    }

    @Bean
    @Profile("dev")
    public CorsWebFilter devCorsWebFilter() {
        return createCorsWebFilter(getDevAllowedOrigins());
    }

    private CorsWebFilter createCorsWebFilter(List<String> allowedOrigins) {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(allowedOrigins);
        corsConfig.setMaxAge(3600L);
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

    private List<String> getLocalAllowedOrigins() {
        return List.of(
                "http://localhost:8080",
                "http://localhost:8081",
                "http://localhost:8082",
                "http://localhost:8086"
        );
    }

    private List<String> getDevAllowedOrigins() {
        return List.of(
                "http://dev.do-flex.co.kr:8080",
                "http://dev.do-flex.co.kr:8081",
                "http://dev.do-flex.co.kr:8082",
                "http://dev.do-flex.co.kr:8086"
        );
    }
}
