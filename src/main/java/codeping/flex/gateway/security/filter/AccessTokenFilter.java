package codeping.flex.gateway.security.filter;

import codeping.flex.gateway.global.common.exception.ApplicationException;
import codeping.flex.gateway.global.common.response.ApplicationResponse;
import codeping.flex.gateway.global.common.response.code.CommonErrorCode;
import codeping.flex.gateway.global.properties.ServerDomainProperties;
import codeping.flex.gateway.security.jwt.access.AccessTokenValidator;
import codeping.flex.gateway.security.passport.PassportRequestDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static codeping.flex.gateway.security.jwt.AuthConstants.PASSPORT_HEADER_PREFIX;
import static codeping.flex.gateway.security.jwt.AuthConstants.TOKEN_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessTokenFilter implements GlobalFilter {
    private static final int TOKEN_EXPIRATION_MINUTES = 30;

    private final ServerDomainProperties serverDomainProperties;
    private final AccessTokenValidator accessTokenValidator;
    private final ObjectMapper objectMapper;
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

        return Mono.justOrEmpty(accessTokenValidator.extractToken(request))
                .flatMap(accessTokenValidator::validateToken)
                .flatMap(token -> processPassportData(exchange, token, path))
                .flatMap(chain::filter)
                .onErrorResume(error -> handleError(exchange, error));
    }

    /**
     * 주어진 경로가 인증이 필요 없는 엔드포인트인지 확인하여
     * 인증이 필요 없는 경로이면 true, 그렇지 않으면 false를 반환합니다.
     * @param path 검사할 URI 경로
     * @return 인증이 필요한 경로인지 여부
     */
    private boolean isAnonymousEndpoint(String path) {
        return Stream.of(WebSecurityUrl.READ_ONLY_PUBLIC_ENDPOINTS,
                        WebSecurityUrl.ANONYMOUS_ENDPOINTS,
                        WebSecurityUrl.SWAGGER_ENDPOINTS)
                .flatMap(Arrays::stream)
                .anyMatch(endpoint -> pathMatcher.match(endpoint, path));
    }

    private Mono<ServerWebExchange> processPassportData(ServerWebExchange exchange, String token, String path) {
        return getPassportData(token, PassportRequestDto.of(path, LocalDateTime.now().plusMinutes(TOKEN_EXPIRATION_MINUTES)))
                .flatMap(passportData -> addPassportHeaders(exchange, passportData));
    }

    /**
     * 검증한 토큰에 대한 사용자의 Passport를 발급하여 반환합니다.
     * @param token Bearer 토큰
     * @return 패스포트 데이터를 포함한 Mono<Map>
     */
    private Mono<Map<String, String>> getPassportData(String token, PassportRequestDto requestDto) {
        return webClient.post()
                .uri(serverDomainProperties.getService() + "/api/passport")
                .header(HttpHeaders.AUTHORIZATION, TOKEN_PREFIX + token)
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {
                });
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
     * @param exchange 현재 서버 웹 교환
     * @param error 발생한 에러
     * @return 에러 응답을 포함한 Mono<Void>
     */
    private Mono<Void> handleError(ServerWebExchange exchange, Throwable error) {
        HttpStatus status;
        CommonErrorCode errorCode;

        if (error instanceof ApplicationException) {
            status = HttpStatus.UNAUTHORIZED;
            errorCode = CommonErrorCode.UNAUTHORIZED;
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApplicationResponse<Object> body = ApplicationResponse.onFailure(errorCode.getCustomCode(), errorCode.getMessage(), null);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            log.error("Error while serializing error response", e);
            bytes = errorCode.getMessage().getBytes();
        }

        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory().wrap(bytes)));
    }
}