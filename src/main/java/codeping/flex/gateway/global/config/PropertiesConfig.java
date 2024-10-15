package codeping.flex.gateway.global.config;

import codeping.flex.gateway.global.properties.ServerDomainProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
	ServerDomainProperties.class
})
public class PropertiesConfig {
}