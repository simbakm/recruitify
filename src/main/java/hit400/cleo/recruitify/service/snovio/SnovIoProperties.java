package hit400.cleo.recruitify.service.snovio;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "snovio")
public record SnovIoProperties(
        String baseUrl,
        String clientId,
        String clientSecret,
        String defaultDomain,
        Integer timeoutSeconds,
        Integer pollAttempts,
        Integer pollDelayMs
) {
    public SnovIoProperties {
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://api.snov.io";
        if (defaultDomain != null && defaultDomain.isBlank()) defaultDomain = null;
        if (timeoutSeconds == null) timeoutSeconds = 20;
        if (pollAttempts == null) pollAttempts = 10;
        if (pollDelayMs == null) pollDelayMs = 700;
    }
}
