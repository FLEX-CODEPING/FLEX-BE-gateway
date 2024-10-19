package codeping.flex.gateway.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "server.domain")
public class ServerDomainProperties {
    private final String service;
}
