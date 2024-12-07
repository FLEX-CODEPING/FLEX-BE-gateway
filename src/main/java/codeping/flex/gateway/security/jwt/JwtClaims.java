package codeping.flex.gateway.security.jwt;

import java.util.Map;

public interface JwtClaims {

    Map<String, Object> getClaims();
}
