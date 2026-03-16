package hit400.cleo.recruitify.service.apollo;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(ApolloProperties.class)
public class ApolloWebClientConfig {

    @Bean
    public WebClient apolloWebClient(ApolloProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(properties.timeoutSeconds()));

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(properties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            builder.defaultHeader(properties.apiKeyHeader(), properties.apiKey());
        }

        return builder.build();
    }
}
