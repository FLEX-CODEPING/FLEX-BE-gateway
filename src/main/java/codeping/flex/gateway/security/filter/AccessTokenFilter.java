package codeping.flex.gateway.security.filter;

import static codeping.flex.gateway.security.filter.WebSecurityUrl.EXCLUDE_TOKEN_VALIDATION;
import static codeping.flex.gateway.security.filter.WebSecurityUrl.PASSPORT_ENDPOINT;
import static codeping.flex.gateway.security.jwt.AuthConstants.BEARER;
import static codeping.flex.gateway.security.jwt.AuthConstants.PASSPORT_HEADER_PREFIX;

import codeping.flex.gateway.global.common.exception.ApplicationException;
import codeping.flex.gateway.global.common.response.ApplicationResponse;
import codeping.flex.gateway.global.common.response.code.BaseErrorCode;
import codeping.flex.gateway.global.common.response.code.CommonErrorCode;
import codeping.flex.gateway.global.common.response.code.GatewayErrorCode;
import codeping.flex.gateway.security.jwt.access.AccessTokenValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.PathMatcher;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessTokenFilter implements GlobalFilter {

    private final AccessTokenValidator accessTokenValidator;
    private final ObjectMapper objectMapper;
    private final PathMatcher pathMatcher;
    private final WebClient webClient;

    /**
     * 모든 HTTP 요청에 대해 실행되는 필터 메서드입니다.
     * 인증이 필요한 엔드포인트를 검증하고 Passport 데이터를 헤더에 추가합니다.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 인증이 필요하지 않은 경로
        if (isAnonymousEndpoint(path)) {
            log.debug("Anonymous endpoint detected, skipping authentication");
            return chain.filter(exchange);
        }

        // 토큰 추출 공통 로직
        Mono<String> extractedTokenMono = Mono.justOrEmpty(accessTokenValidator.extractToken(request))
            .switchIfEmpty(Mono.error(ApplicationException.from(GatewayErrorCode.EMPTY_TOKEN)));

        // 엔드포인트에 대한 토큰 검증 필요 유무 판단
        boolean requiresTokenValidation = !isExcludedFromTokenValidation(path);

        return extractedTokenMono
            .flatMap(token -> grantPassportByToken(exchange, chain, token, requiresTokenValidation))
            .onErrorResume(error -> handleError(exchange, error));
    }

    /**
     * 주어진 경로가 토큰 검증에서 제외된 엔드포인트인지 확인합니다.
     * @param path 검사할 URI 경로
     * @return 토큰 검증에서 제외된 경로인지 여부
     */
    private boolean isExcludedFromTokenValidation(String path) {
        return Stream.of(EXCLUDE_TOKEN_VALIDATION)
            .anyMatch(endpoint -> pathMatcher.match(endpoint, path));
    }

    /**
     * 추출된 토큰으로 Passport 데이터를 헤더에 추가합니다.
     * requiresTokenValidation 값에 따라 토큰 검증 처리를 포함합니다.
     */
    private Mono<Void> grantPassportByToken(ServerWebExchange exchange, GatewayFilterChain chain, String token, boolean requiresTokenValidation) {
        Mono<String> validTokenMono = requiresTokenValidation
            ? accessTokenValidator.validateToken(token)
            : Mono.just(token);

        return validTokenMono
            .flatMap(validToken -> processPassportData(exchange, validToken))
            .flatMap(chain::filter);
    }

    /**
     * 주어진 경로가 인증이 필요 없는 엔드포인트인지 확인하여
     * 인증이 필요 없는 경로이면 true, 그렇지 않으면 false 를 반환합니다.
     * @param path 검사할 URI 경로
     * @return 인증이 필요한 경로인지 여부
     */
    private boolean isAnonymousEndpoint(String path) {
        return Stream.of(
            WebSecurityUrl.READ_ONLY_PUBLIC_ENDPOINTS, WebSecurityUrl.ANONYMOUS_ENDPOINTS, WebSecurityUrl.SWAGGER_ENDPOINTS
            )
            .flatMap(Arrays::stream)
            .peek(endpoint -> log.debug("Checking endpoint: {}, Match result: {}", endpoint, pathMatcher.match(endpoint, path)))
            .anyMatch(endpoint -> pathMatcher.match(endpoint, path));
    }

    private Mono<ServerWebExchange> processPassportData(ServerWebExchange exchange, String accessToken) {
        return getPassportData(accessToken)
            .flatMap(passportData -> addPassportHeaders(exchange, passportData));
    }

    /**
     * 검증한 토큰에 대한 사용자의 Passport를 발급 받아 반환합니다.
     * @param accessToken Bearer 토큰
     * @return 패스포트 데이터를 포함한 Mono<Map>
     */
    private Mono<Map<String, String>> getPassportData(String accessToken) {
        return webClient.get()
            .uri(PASSPORT_ENDPOINT)
            .header(HttpHeaders.AUTHORIZATION, BEARER.getValue() + " " + accessToken)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    /**
     * 발급받은 Passport를 요청 헤더에 추가합니다.
     * @param passport
     */
    private Mono<ServerWebExchange> addPassportHeaders(ServerWebExchange exchange, Map<String, String> passport) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        passport.forEach((key, value) -> builder.header(PASSPORT_HEADER_PREFIX.getValue() + key, value));
        ServerHttpRequest newRequest = builder.build();

        log.info("Processed Passport:");
        newRequest.getHeaders().forEach((key, values) -> {
            log.info("  {}: {}", key, String.join(", ", values));
        });
        return Mono.just(exchange.mutate().request(newRequest).build());
    }

    /**
     * 에러 처리 메서드
     * @param exchange 현재 서버 웹 교환
     * @param error 발생한 에러
     * @return 에러 응답을 포함한 Mono<Void>
     */
    private Mono<Void> handleError(ServerWebExchange exchange, Throwable error) {
        log.error("Error occurred in AccessTokenFilter: ", error);
        HttpStatus status;
        BaseErrorCode errorCode;

        if (error instanceof ApplicationException) {
            status = HttpStatus.UNAUTHORIZED;
            errorCode = ((ApplicationException) error).getCode();
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] errorResponse = createErrorResponse(errorCode);

        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(errorResponse)));
    }

    private byte[] createErrorResponse(BaseErrorCode errorCode) {
        ApplicationResponse<Object> body = ApplicationResponse.onFailure(errorCode.getCustomCode(), errorCode.getMessage(), null);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            log.error("Error while serializing error response", e);
            bytes = errorCode.getMessage().getBytes();
        }
        return bytes;
    }
}