package codeping.flex.gateway.global.properties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "server.domain")
public class ServerDomainProperties {
    private final String user;
    private final String service;
}
