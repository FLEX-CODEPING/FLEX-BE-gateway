package codeping.flex.gateway.security.jwt.access;

import codeping.flex.gateway.global.common.exception.ApplicationException;
import codeping.flex.gateway.global.common.response.code.GatewayErrorCode;
import codeping.flex.gateway.security.jwt.AuthConstants;
import codeping.flex.gateway.security.jwt.JwtClaims;
import codeping.flex.gateway.security.jwt.TokenValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;

import static codeping.flex.gateway.security.jwt.access.AccessTokenClaimKeys.USER_ID;

@Slf4j
@Component
public class AccessTokenValidator implements TokenValidator {

    @Value("${jwt.secret-key}")
    private String secretKey;

    /**
     * HTTP 요청 헤더에서 에서 Bearer 토큰을 추출합니다.
     * @param request HTTP 요청
     * @return 추출된 토큰, 없으면 null
     */
    public String extractToken(ServerHttpRequest request) {
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
    public Mono<String> validateToken(String token) {
        return Mono.fromCallable(() -> {
                    if(!StringUtils.hasText(token)){
                        throw new ApplicationException(GatewayErrorCode.EMPTY_TOKEN);
                    }
                    Claims claims = getClaimsFromToken(token);
                    if (isTokenExpired(claims)) {
                        throw new ApplicationException(GatewayErrorCode.EXPIRED_TOKEN);
                    }
                    return token;
                }).doOnSuccess(t -> log.debug("토큰 검증 성공"))
                .doOnError(e -> log.error("토큰 검증 실패", e));
    }

    @Override
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.warn("Token is invalid: {}", e.getMessage());
            throw new ApplicationException(GatewayErrorCode.INVALID_TOKEN);
        }
    }

    @Override
    public boolean isTokenExpired(Claims claims) {
        try {
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.error("Token is expired: {}", e.getMessage());
            throw e;
        }
    }

}