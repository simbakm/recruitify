package hit400.cleo.recruitify.service.leadiq;
 
import org.springframework.boot.context.properties.ConfigurationProperties;
 
@ConfigurationProperties(prefix = "leadiq")
public record LeadIQProperties(
        String baseUrl,
        String apiKey,
        Integer defaultLimit,
        Integer timeoutSeconds
) {
    public LeadIQProperties {
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://api.leadiq.com/graphql";
        if (defaultLimit == null) defaultLimit = 25;
        if (timeoutSeconds == null) timeoutSeconds = 20;
    }
}
