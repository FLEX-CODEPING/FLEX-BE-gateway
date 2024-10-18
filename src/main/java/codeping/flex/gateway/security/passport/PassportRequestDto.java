package codeping.flex.gateway.security.passport;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

public record PassportRequestDto(
        @JsonProperty("passportId") String id,
        @JsonProperty("destination") String path,
        @JsonProperty("expirationTime") LocalDateTime expirationTime
) {
    public static PassportRequestDto of(String path, LocalDateTime expirationTime) {
        return new PassportRequestDto(UUID.randomUUID().toString(), path, expirationTime);
    }
}