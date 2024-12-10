package codeping.flex.gateway.security.filter;

public class WebSecurityUrl {

    private WebSecurityUrl() {
        throw new IllegalStateException("Utility class");
    }

    protected static final String[] READ_ONLY_PUBLIC_ENDPOINTS = {"/favicon.ico"};
    protected static final String[] ANONYMOUS_ENDPOINTS = {
            "/api/auth/login/**",
            "/api/auth/signup/**",
            "/api/blogs/landings/latest",
            "/api/blogs/landings/popular",
            "/api/users/checkBlogName",
            "/api/kis/stocks/**",
            "/api/news-summary/todaynews"

    };
    protected static final String[] EXCLUDE_TOKEN_VALIDATION = {"/api/auth/reissue"};
    protected static final String[] SWAGGER_ENDPOINTS = {
            "/api/v3/api-docs/**",
            "/api/swagger-ui/**",
            "/api/swagger-ui.html",
            "/api/webjars/**",
            "/swagger-resources/**",
            "/api/blog-service/v3/api-docs",
            "/api/user-service/v3/api-docs",
            "/api/news-service/openapi.json",
            "/api/stock-service/v3/api-docs",
            "/api/investment-service/v3/api-docs",
            "/api/stock-integration-service/v3/api-docs",
            "/api/stock-integration-service/openapi.json",
            "/api/image-service/openapi.json",
            "/api/stock-test-service/openapi.json"
    };

    protected static final String PASSPORT_ENDPOINT = "/api/passport";
}