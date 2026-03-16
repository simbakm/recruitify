package hit400.cleo.recruitify.service.apollo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "apollo")
public record ApolloProperties(
        String baseUrl,
        String apiKey,
        String apiKeyHeader,
        String peopleSearchPath,
        Integer defaultPageSize,
        Integer timeoutSeconds
) {
    public ApolloProperties {
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://api.apollo.io";
        if (apiKeyHeader == null || apiKeyHeader.isBlank()) apiKeyHeader = "X-Api-Key";
        if (peopleSearchPath == null || peopleSearchPath.isBlank()) peopleSearchPath = "/v1/mixed_people/search";
        if (defaultPageSize == null) defaultPageSize = 25;
        if (timeoutSeconds == null) timeoutSeconds = 20;
    }
}

