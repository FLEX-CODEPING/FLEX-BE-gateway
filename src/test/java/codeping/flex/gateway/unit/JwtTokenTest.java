package codeping.flex.gateway.unit;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.Date;
import io.jsonwebtoken.io.Decoders;
import static org.junit.jupiter.api.Assertions.*;

public class JwtTokenTest {

    private static final String SECRET = "943cb527164de7a08667b48ae9279a3888545bdcb83f1da6d178370eb015aecb";
    private static final long ACCESS_EXPIRATION = 1800;
    private static final String TYPE = "type";
    private static final String ROLE = "role";
    private static final String USER = "user";
    private static final String EMAIL = "email";

    private SecretKey key;

    @BeforeEach
    void setUp() {
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }

    @Test
    void testGenerateAndValidateToken() {
        // Generate token
        String token = generateAccessToken();
        assertNotNull(token);

        System.out.println(token);
        // Validate token
        Claims claims = getClaimsFromToken(token);
        
        assertEquals("1", claims.getSubject());
        assertEquals(USER, claims.get(ROLE));
        assertEquals("ACCESS", claims.get(TYPE));
        assertEquals("joowojr@gmail.com", claims.get(EMAIL));
        
        // Check expiration
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    void testExpiredToken() {
        // Generate expired token
        String expiredToken = generateExpiredToken();
        
        // Attempt to validate expired token
        Exception exception = assertThrows(Exception.class, () -> {
            getClaimsFromToken(expiredToken);
        });
        
        assertTrue(exception.getMessage().contains("expired"));
    }

    private String generateAccessToken() {
        Instant accessDate = LocalDateTime.now().plusSeconds(ACCESS_EXPIRATION).atZone(ZoneId.systemDefault()).toInstant();

        return Jwts.builder()
            .setSubject(String.valueOf(1L))
            .claim(ROLE, USER)
            .claim(TYPE, "ACCESS")
            .claim(EMAIL, "joowojr@gmail.com")
            .setExpiration(Date.from(accessDate))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    private String generateExpiredToken() {
        Instant expiredDate = LocalDateTime.now().minusSeconds(1).atZone(ZoneId.systemDefault()).toInstant();

        return Jwts.builder()
            .setSubject(String.valueOf(1L))
            .claim(ROLE, USER)
            .claim(TYPE, "ACCESS")
            .claim(EMAIL, "joowojr@gmail.com")
            .setExpiration(Date.from(expiredDate))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}