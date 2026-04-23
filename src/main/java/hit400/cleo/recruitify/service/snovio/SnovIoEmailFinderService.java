package hit400.cleo.recruitify.service.snovio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SnovIoEmailFinderService {

    private static final Logger log = LogManager.getLogger(SnovIoEmailFinderService.class);

    private final WebClient snovIoWebClient;
    private final SnovIoAuthService authService;
    private final SnovIoProperties properties;
    private final ObjectMapper objectMapper;

    public record NameDomainRow(String firstName, String lastName, String domain) {}

    public record EmailFinderResult(
            String fullName,
            String domain,
            String email
    ) {}

    public Mono<EmailFinderResult> findEmailByFullName(String fullName, String domain) {
        if (fullName == null || fullName.isBlank()) {
            return Mono.error(new IllegalArgumentException("fullName is required"));
        }
        if (domain == null || domain.isBlank()) {
            return Mono.error(new IllegalArgumentException("domain is required"));
        }

        String[] parts = fullName.trim().split("\\s+");
        if (parts.length < 2) {
            return Mono.error(new IllegalArgumentException("fullName must include at least first and last name"));
        }
        String firstName = parts[0].trim();
        String lastName = parts[parts.length - 1].trim();

        return findEmailsByNameAndDomain(List.of(new NameDomainRow(firstName, lastName, domain.trim())))
                .map(map -> Optional.ofNullable(map.get((fullName.trim().toLowerCase(Locale.ROOT)))))
                .map(opt -> new EmailFinderResult(fullName.trim(), domain.trim(), opt.orElse(null)));
    }

    public Mono<Map<String, String>> findEmailsByNameAndDomain(List<NameDomainRow> rows) {
        if (rows == null || rows.isEmpty()) return Mono.just(Map.of());

        List<NameDomainRow> clean = rows.stream()
                .filter(r -> r != null
                        && r.firstName() != null && !r.firstName().isBlank()
                        && r.lastName() != null && !r.lastName().isBlank()
                        && r.domain() != null && !r.domain().isBlank())
                .toList();

        if (clean.isEmpty()) return Mono.just(Map.of());

        return Flux.fromIterable(partition(clean, 10))
                .concatMap(this::findEmailsBatch)
                .reduce(new HashMap<String, String>(), (acc, next) -> {
                    acc.putAll(next);
                    return acc;
                })
                .map(Map::copyOf);
    }

    private Mono<Map<String, String>> findEmailsBatch(List<NameDomainRow> rows) {
        return authService.getAccessToken()
                .flatMap(token -> startTask(token, rows)
                        .flatMap(taskHash -> pollResult(token, taskHash, properties.pollAttempts()))
                )
                .onErrorResume(e -> {
                    log.error("Snov.io email lookup failed", e);
                    return Mono.just(Map.of());
                });
    }

    private Mono<String> startTask(String token, List<NameDomainRow> rows) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("rows", rows.stream()
                .map(r -> Map.of(
                        "first_name", r.firstName(),
                        "last_name", r.lastName(),
                        "domain", r.domain()
                ))
                .toList());

        return snovIoWebClient
                .post()
                .uri("/v2/emails-by-domain-by-name/start")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new SnovIoException(response.statusCode(), body)))
                )
                .bodyToMono(String.class)
                .flatMap(body -> Mono.fromCallable(() -> objectMapper.readTree(body)))
                .map(json -> json.path("data").path("task_hash").asText(""))
                .flatMap(taskHash -> taskHash.isBlank()
                        ? Mono.error(new IllegalStateException("Snov.io response missing task_hash"))
                        : Mono.just(taskHash));
    }

    private Mono<Map<String, String>> pollResult(String token, String taskHash, int attemptsLeft) {
        return fetchResult(token, taskHash)
                .flatMap(result -> {
                    String status = result.path("status").asText("");
                    if ("completed".equalsIgnoreCase(status)) {
                        return Mono.just(parseEmails(result));
                    }
                    if (attemptsLeft <= 1) {
                        return Mono.just(Map.of());
                    }
                    return Mono.delay(Duration.ofMillis(properties.pollDelayMs()))
                            .then(pollResult(token, taskHash, attemptsLeft - 1));
                });
    }

    private Mono<JsonNode> fetchResult(String token, String taskHash) {
        return snovIoWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/emails-by-domain-by-name/result")
                        .queryParam("task_hash", taskHash)
                        .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new SnovIoException(response.statusCode(), body)))
                )
                .bodyToMono(String.class)
                .flatMap(body -> Mono.fromCallable(() -> objectMapper.readTree(body)));
    }

    private static Map<String, String> parseEmails(JsonNode json) {
        JsonNode data = json.get("data");
        if (data == null || !data.isArray()) return Map.of();

        Map<String, String> emailsByPerson = new HashMap<>();
        for (JsonNode personNode : data) {
            String people = personNode.path("people").asText("");
            JsonNode results = personNode.path("result");
            if (people.isBlank() || results == null || !results.isArray() || results.isEmpty()) continue;
            String email = results.get(0).path("email").asText("");
            if (!email.isBlank()) {
                emailsByPerson.put(normalizePersonKey(people), email);
            }
        }
        return emailsByPerson;
    }

    private static String normalizePersonKey(String fullName) {
        return fullName == null ? "" : fullName.trim().toLowerCase(Locale.ROOT);
    }

    private static <T> List<List<T>> partition(List<T> items, int size) {
        if (items == null || items.isEmpty()) return List.of();
        int chunk = Math.max(1, size);
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < items.size(); i += chunk) {
            out.add(items.subList(i, Math.min(items.size(), i + chunk)));
        }
        return out;
    }
}
