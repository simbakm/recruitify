package hit400.cleo.recruitify.service.serpapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "serpapi")
public record SerpApiProperties(
        String baseUrl,
        String apiKey,
        String engine,
        String googleDomain,
        String location,
        String hl,
        String gl,
        String safe,
        Integer minLimit,
        Integer defaultLimit,
        Integer timeoutSeconds,
        String defaultSite
) {
    public SerpApiProperties {
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://serpapi.com";
        if (engine == null || engine.isBlank()) engine = "google";
        if (googleDomain != null && googleDomain.isBlank()) googleDomain = null;
        if (location != null && location.isBlank()) location = null;
        if (hl != null && hl.isBlank()) hl = null;
        if (gl != null && gl.isBlank()) gl = null;
        if (safe == null || safe.isBlank()) safe = "active";
        if (minLimit == null) minLimit = 5;
        if (defaultLimit == null) defaultLimit = 10;
        if (timeoutSeconds == null) timeoutSeconds = 20;
        if (defaultSite == null || defaultSite.isBlank()) defaultSite = "linkedin.com/in";
    }
}
