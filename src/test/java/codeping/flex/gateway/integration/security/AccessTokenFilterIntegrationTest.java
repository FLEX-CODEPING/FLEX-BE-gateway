package codeping.flex.gateway.integration.security;

import codeping.flex.gateway.global.common.response.code.GatewayErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.stream.Stream;

import static codeping.flex.gateway.integration.security.WebSecurityUrl.ANONYMOUS_ENDPOINTS;
import static codeping.flex.gateway.security.jwt.AuthConstants.AUTHORIZATION;
import static codeping.flex.gateway.security.jwt.AuthConstants.BEARER;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
public class AccessTokenFilterIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    static Stream<String> anonymousEndpointsProvider() {
        return Stream.of(ANONYMOUS_ENDPOINTS);
    }
    /**
     * 익명 사용자의 로그인 엔드포인트 접근 테스트
     */
    @ParameterizedTest
    @MethodSource("anonymousEndpointsProvider")
    void testAnonymousEndpoints(String uri) {
        webTestClient.post().uri(uri)
                .exchange()
                .expectStatus().isOk();
    }

    /**
     * 토큰 없이 보호된 엔드포인트 접근 테스트
     * 토큰이 없으면 인증되지 않은 상태로 간주되어야 함
     */
    @Test
    void testProtectedEndpointWithoutToken() {
        webTestClient.get().uri("/api/users/profile")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.isSuccess").isEqualTo(false)
                .jsonPath("$.code").isEqualTo(GatewayErrorCode.EMPTY_TOKEN.getCustomCode());
    }

    /**
     * 잘못된 토큰으로 보호된 엔드포인트 접근 테스트
     * 유효하지 않은 토큰으로 요청 시 인증 실패 응답을 받아야 함
     */
    @Test
    void testProtectedEndpointWithInvalidToken() {
        webTestClient.get().uri("/api/users/profile")
                .header(AUTHORIZATION.getValue(), BEARER.getValue()+"invalid_token")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.isSuccess").isEqualTo(false)
                .jsonPath("$.code").isEqualTo(GatewayErrorCode.INVALID_TOKEN.getCustomCode());
    }
}