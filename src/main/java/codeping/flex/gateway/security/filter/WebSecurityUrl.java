package codeping.flex.gateway.security.filter;

public class WebSecurityUrl {
    private WebSecurityUrl() {
        throw new IllegalStateException("Utility class");
    }

    protected static final String[] READ_ONLY_PUBLIC_ENDPOINTS = {"/favicon.ico"};
    protected static final String[] ANONYMOUS_ENDPOINTS = {"/api/auth/login/**", "/api/auth/signup"};
    protected static final String[] SWAGGER_ENDPOINTS = {
            "/api/v3/api-docs/**",
            "/api/swagger-ui/**",
            "/api/swagger-ui.html",
            "/api/webjars/**",
            "/swagger-resources/**",
            "/api/blog-service/v3/api-docs",
            "/api/user-service/v3/api-docs",
            "/api/ai-news-service/v3/api-docs",
            "/api/stock-service/v3/api-docs",
            "/api/investment-service/v3/api-docs",
            "/api/image-service/v3/api-docs"
    };
    protected static final String PASSPORT_ENDPOINT = "/api/passport";
    protected static final String[] REISSUANCE_ENDPOINTS = {"/api/v1/auth/reissuance"};
}