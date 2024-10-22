//package codeping.flex.gateway.global.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Profile;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.reactive.CorsWebFilter;
//import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
//
//import java.util.Arrays;
//
//@Configuration
//public class CorsConfig {
//
//    @Bean
//    @Profile("local")
//    public CorsWebFilter localCorsWebFilter() {
//        return createCorsWebFilter(
//                "http://localhost:3000",
//                "http://localhost:8080",
//                "http://localhost:8081",
//                "http://localhost:40020",
//                "http://127.0.0.1:40020"
//        );
//    }
//
//    @Bean
//    @Profile("dev")
//    public CorsWebFilter devCorsWebFilter() {
//        return createCorsWebFilter(
//                "http://localhost:3000",
//                "http://dev.do-flex.co.kr:8080",
//                "http://dev.do-flex.co.kr:8081"
//        );
//    }
//
//    private CorsWebFilter createCorsWebFilter(String... allowedOrigins) {
//        CorsConfiguration corsConfig = new CorsConfiguration();
//        corsConfig.setAllowedOrigins(Arrays.asList(allowedOrigins));
//        corsConfig.setMaxAge(3600L);
//        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        corsConfig.setAllowedHeaders(Arrays.asList("*"));
//        corsConfig.setAllowCredentials(true);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", corsConfig);
//
//        return new CorsWebFilter(source);
//    }
//}