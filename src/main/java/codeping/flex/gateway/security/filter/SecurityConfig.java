package codeping.flex.gateway.security.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(WebSecurityUrl.READ_ONLY_PUBLIC_ENDPOINTS).permitAll()
                        .pathMatchers(WebSecurityUrl.SWAGGER_ENDPOINTS).permitAll()
                        .pathMatchers(WebSecurityUrl.ANONYMOUS_ENDPOINTS).permitAll()
                        .anyExchange().authenticated()
                );

        return http.build();
    }
}