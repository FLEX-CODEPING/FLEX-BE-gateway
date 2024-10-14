package codeping.flex.gateway.security.jwt;

import io.jsonwebtoken.Claims;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

import static codeping.flex.gateway.security.jwt.AuthConstants.TOKEN_PREFIX;

public interface TokenValidator {
    /**
     * 헤더로부터 토큰을 추출하고 유효선을 검증하는 메서드
     *
     * @param header : Authorization 헤더
     * @return 값이 있다면 토큰, 없다면 빈 문자열
     */
    default String resolveToken(String header) {
        if (StringUtils.hasText(header) && header.startsWith(AuthConstants.TOKEN_PREFIX.getValue())) {
            return header.substring(TOKEN_PREFIX.getValue().length());
        }
        return "";
    }

    /**
     * 토큰을 파싱하여 JwtClaims 객체로 변환하는 메서드
     *
     * @param token : 토큰
     * @return 사용자 정보를 담은 {@link JwtClaims} 객체
     */
    JwtClaims parseJwtClaimsFromToken(String token);


    /**
     * 토큰이 만료되었는지 확인하는 메서드
     *
     * @param claims : 토큰으로 부터 추출한 클레임
     */
    boolean isTokenExpired(Claims claims);

    /**
     * 토큰에서 클레임을 추출하는 메서드
     *
     * @param token : 토큰
     * @return 사용자 정보를 담은 {@link Claims} 객체
     */
    Claims getClaimsFromToken(String token);

}
