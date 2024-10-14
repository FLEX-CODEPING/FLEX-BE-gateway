package codeping.flex.gateway.security.filter;

import codeping.flex.gateway.global.common.exception.ApplicationException;
import codeping.flex.gateway.global.common.response.code.CommonErrorCode;
import codeping.flex.gateway.global.common.response.code.GatewayErrorCode;
import codeping.flex.gateway.global.properties.ServerDomainProperties;
import codeping.flex.gateway.security.jwt.AuthConstants;
import codeping.flex.gateway.security.jwt.access.AccessTokenValidator;
import codeping.flex.gateway.security.handler.JwtAuthenticationEntryPoint;
import codeping.flex.gateway.security.passport.PassportRequestDto;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static codeping.flex.gateway.security.jwt.AuthConstants.PASSPORT_HEADER_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessTokenFilter implements GlobalFilter {
    private final ServerDomainProperties serverDomainProperties;
    private final AccessTokenValidator accessTokenValidator;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final PathMatcher pathMatcher;
    private final WebClient webClient;

    /**
     * 모든 HTTP 요청에 대해 실행되는 필터 메서드입니다.
     * 인증이 필요한 엔드포인트에 대해 토큰을 검증하고 Passport 데이터를 헤더에 추가합니다.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 인증이 필요하지 않은 path 는 filter를 거치지 않음
        if (isAnonymousEndpoint(path)) {
            return chain.filter(exchange);
        }

        return Mono.justOrEmpty(extractToken(request))
                .flatMap(this::validateToken)
                .flatMap(token -> getPassportData(token, PassportRequestDto.of(path, LocalDateTime.now().plusMinutes(30)))
                .flatMap(passportData -> addPassportHeaders(exchange, passportData))
                .flatMap(chain::filter)
                .onErrorResume(error -> handleError(exchange,error)));
    }

    /**
     * 주어진 경로가 인증이 필요 없는 엔드포인트인지 확인하여
     * 인증이 필요 없는 경로이면 true, 그렇지 않으면 false를 반환합니다.
     * @param path 검사할 URI 경로
     * @return 인증이 필요한 경로인지 여부
     */
    private boolean isAnonymousEndpoint(String path) {
        return Arrays.stream(WebSecurityUrl.READ_ONLY_PUBLIC_ENDPOINTS).anyMatch(endpoint -> pathMatcher.match(endpoint, path))
                || Arrays.stream(WebSecurityUrl.ANONYMOUS_ENDPOINTS).anyMatch(endpoint -> pathMatcher.match(endpoint, path))
                || Arrays.stream(WebSecurityUrl.SWAGGER_ENDPOINTS).anyMatch(endpoint -> pathMatcher.match(endpoint, path));
    }
    /**
     * HTTP 요청 헤더에서 에서 Bearer 토큰을 추출합니다.
     * @param request HTTP 요청
     * @return 추출된 토큰, 없으면 null
     */
    private String extractToken(ServerHttpRequest request) {
        List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (StringUtils.hasText(authHeader) && authHeader.startsWith(AuthConstants.TOKEN_PREFIX.getValue())) {
                return authHeader.substring(AuthConstants.TOKEN_PREFIX.getValue().length());
            }
        }
        return null;
    }

    /**
     * 추출한 JWT 토큰의 유효성을 검사합니다.
     * 토큰이 있는지, 유효한 토큰인지, 만료된 토큰인지를 검증합니다.
     * @param token JWT 토큰
     * @return 유효한 토큰이면 Mono<String>
     */
    private Mono<String> validateToken(String token) {
        return Mono.fromCallable(() -> {
                    if(!StringUtils.hasText(token)){
                        throw new ApplicationException(GatewayErrorCode.EMPTY_TOKEN);
                    }
                    Claims claims = accessTokenValidator.getClaimsFromToken(token);
                    if (accessTokenValidator.isTokenExpired(claims)) {
                        throw new ApplicationException(GatewayErrorCode.EXPIRED_TOKEN);
                    }
                    return token;
                }).doOnSuccess(t -> log.debug("토큰 검증 성공"))
                .doOnError(e -> log.error("토큰 검증 실패", e));
    }

    /**
     * 검증한 토큰에 대한 사용자의 Passport를 발급하여 반환합니다.
     * @param authHeader 인증 헤더 (Bearer 토큰 포함)
     * @return 패스포트 데이터를 포함한 Mono<Map>
     */
    private Mono<Map<String, String>> getPassportData(String authHeader, PassportRequestDto requestDto) {
        return webClient.post()
                .uri(serverDomainProperties.getService() + "/api/passport")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {});
    }

    /**
     * 발급한 Paaport를 요청 헤더에 추가합니다.
     * @param passport
     */
    private Mono<ServerWebExchange> addPassportHeaders(ServerWebExchange exchange, Map<String, String> passport) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        passport.forEach((key, value) ->
                builder.header(PASSPORT_HEADER_PREFIX.getValue() + key, value));
        ServerHttpRequest newRequest = builder.build();
        return Mono.just(exchange.mutate().request(newRequest).build());
    }

    /**
     * 에러 처리 메서드
     * JWT 관련 예외는 JwtAuthenticationEntryPoint로 위임하고, 그 외의 에러는 500 에러로 처리합니다.
     * @param exchange 현재 서버 웹 교환
     * @param error 발생한 에러
     * @return 에러 응답을 포함한 Mono<Void>
     */
    private Mono<Void> handleError(ServerWebExchange exchange, Throwable error) {
        if (error instanceof ApplicationException) {
            return jwtAuthenticationEntryPoint.commence(exchange,
                    new BadCredentialsException(error.getMessage()));
        }
        return onError(exchange, CommonErrorCode.INTERNAL_SERVER_ERROR);
    }

    private Mono<Void> onError(ServerWebExchange exchange, CommonErrorCode error) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(error.getHttpStatus());
        return response.writeWith(Mono.just(response.bufferFactory().wrap(error.getMessage().getBytes())));
    }
}