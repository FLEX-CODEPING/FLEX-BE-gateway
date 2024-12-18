package codeping.flex.gateway.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class WebClientConfig {

    @Value("webclient.user-service.base-url")
    private String userServiceBaseUrl;

    @Bean
    public ReactorResourceFactory resourceFactory() {
        return new ReactorResourceFactory();
    }

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        Function<HttpClient, HttpClient> mapper = client -> HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .doOnConnected(connection -> connection.addHandlerLast(new ReadTimeoutHandler(15))
                        .addHandlerLast(new WriteTimeoutHandler(15)))
                .responseTimeout(Duration.ofSeconds(15));

        ClientHttpConnector connector = new ReactorClientHttpConnector(resourceFactory(), mapper);

        return WebClient.builder()
                .clientConnector(connector)
                .filter((request, next) -> {
                    ClientRequest filtered = ClientRequest.from(request)
                            .header(HttpHeaders.AUTHORIZATION, request.headers().getFirst(HttpHeaders.AUTHORIZATION))
                            .build();
                    return next.exchange(filtered)
                            .doOnError(error -> log.error("Error during WebClient request: ", error));
                });
    }

    @Bean
    public WebClient userServiceClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(userServiceBaseUrl)
                .build();
    }
}