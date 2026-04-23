package hit400.cleo.recruitify.service.snovio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class SnovIoAuthService {

    private static final Logger log = LogManager.getLogger(SnovIoAuthService.class);

    private final WebClient snovIoWebClient;
    private final SnovIoProperties properties;
    private final ObjectMapper objectMapper;

    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

    public Mono<String> getAccessToken() {
        CachedToken cached = cachedToken.get();
        if (cached != null && cached.isValid()) {
            return Mono.just(cached.token());
        }
        return fetchAccessToken()
                .doOnNext(token -> cachedToken.set(token))
                .map(CachedToken::token);
    }

    private Mono<CachedToken> fetchAccessToken() {
        if (properties.clientId() == null || properties.clientId().isBlank()) {
            return Mono.error(new IllegalStateException("Snov.io client id is missing. Set snovio.client-id (or SNOVIO_CLIENT_ID)."));
        }
        if (properties.clientSecret() == null || properties.clientSecret().isBlank()) {
            return Mono.error(new IllegalStateException("Snov.io client secret is missing. Set snovio.client-secret (or SNOVIO_CLIENT_SECRET)."));
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());

        return snovIoWebClient
                .post()
                .uri("/v1/oauth/access_token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new SnovIoException(response.statusCode(), body)))
                )
                .bodyToMono(String.class)
                .flatMap(body -> Mono.fromCallable(() -> objectMapper.readTree(body)))
                .map(json -> {
                    String token = json.path("access_token").asText("");
                    long expiresIn = json.path("expires_in").asLong(0L);
                    if (token.isBlank()) {
                        throw new IllegalStateException("Snov.io token response missing access_token");
                    }
                    // Refresh a bit earlier than actual expiry
                    Instant expiresAt = Instant.now().plusSeconds(Math.max(0, expiresIn - 30));
                    return new CachedToken(token, expiresAt);
                })
                .doOnSuccess(t -> log.info("Snov.io access token acquired"))
                .doOnError(e -> log.error("Failed to acquire Snov.io access token", e));
    }

    record CachedToken(String token, Instant expiresAt) {
        boolean isValid() {
            return token != null && !token.isBlank() && expiresAt != null && expiresAt.isAfter(Instant.now());
        }
    }
}

