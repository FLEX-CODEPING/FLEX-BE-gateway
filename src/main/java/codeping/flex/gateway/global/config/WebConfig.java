package codeping.flex.gateway.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

@Configuration
public class WebConfig {
    @Bean
    public PathMatcher pathMatcher() {
        return new AntPathMatcher();
    }
}