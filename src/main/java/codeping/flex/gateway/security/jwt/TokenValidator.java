package codeping.flex.gateway.security.jwt;

import io.jsonwebtoken.Claims;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

import static codeping.flex.gateway.security.jwt.AuthConstants.TOKEN_PREFIX;

public interface TokenValidator {

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
