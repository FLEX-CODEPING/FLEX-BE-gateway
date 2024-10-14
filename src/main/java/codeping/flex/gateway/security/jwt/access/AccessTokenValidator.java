package codeping.flex.gateway.security.jwt.access;

import codeping.flex.gateway.global.common.exception.ApplicationException;
import codeping.flex.gateway.global.common.response.code.GatewayErrorCode;
import codeping.flex.gateway.security.jwt.JwtClaims;
import codeping.flex.gateway.security.jwt.TokenValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

import static codeping.flex.gateway.security.jwt.access.AccessTokenClaimKeys.USER_ID;

@Slf4j
@Component
public class AccessTokenValidator implements TokenValidator {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Override
    public JwtClaims parseJwtClaimsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return AccessTokenClaim.of(
                Long.parseLong(claims.get(USER_ID.getValue(), String.class))
        );
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