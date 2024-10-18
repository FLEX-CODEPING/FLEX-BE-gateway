package codeping.flex.gateway.security.jwt;

import lombok.Getter;

@Getter
public enum AuthConstants {

    AUTHORIZATION("Authorization"), TOKEN_PREFIX("Bearer "), PASSPORT_HEADER_PREFIX ("X-PP-");

    private final String value;

    AuthConstants(String value) {
        this.value = value;
    }
}