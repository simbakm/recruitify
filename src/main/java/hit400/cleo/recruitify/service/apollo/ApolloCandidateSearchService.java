package hit400.cleo.recruitify.service.apollo;

import com.fasterxml.jackson.databind.JsonNode;
import hit400.cleo.recruitify.dto.ApolloCandidateSearchRequest;
import hit400.cleo.recruitify.dto.QualifiedCandidateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApolloCandidateSearchService {

    private final WebClient apolloWebClient;
    private final ApolloProperties properties;

    private final CandidateQualificationScorer scorer = new CandidateQualificationScorer();

    public Mono<List<QualifiedCandidateDto>> searchQualifiedCandidates(ApolloCandidateSearchRequest request) {
        if (request == null) {
            return Mono.error(new IllegalArgumentException("Request body is required"));
        }
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            return Mono.error(new IllegalArgumentException("Apollo API key is not configured. Set `apollo.api-key` in application.properties or via environment variables."));
        }

        int maxResults = request.maxResults() == null ? 25 : Math.max(1, Math.min(200, request.maxResults()));
        boolean requireAllRequired = request.requireAllRequiredSkills() == null || request.requireAllRequiredSkills();

        Map<String, Object> payload = buildApolloPayload(request, maxResults);

        return apolloWebClient
                .post()
                .uri(properties.peopleSearchPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new ApolloApiException(response.statusCode(), body)))
                )
                .bodyToMono(JsonNode.class)
                .map(json -> toQualifiedCandidates(json, request, requireAllRequired))
                .map(list -> list.stream()
                        .sorted(Comparator.comparingDouble(QualifiedCandidateDto::matchScore).reversed())
                        .limit(maxResults)
                        .toList());
    }

    private Map<String, Object> buildApolloPayload(ApolloCandidateSearchRequest request, int maxResults) {
        ApolloCandidateSearchRequest.ApolloOverrides overrides = request.apollo();

        int perPage = overrides != null && overrides.perPage() != null
                ? overrides.perPage()
                : properties.defaultPageSize();
        perPage = Math.max(1, Math.min(200, perPage));

        int page = overrides != null && overrides.page() != null ? Math.max(1, overrides.page()) : 1;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("page", page);
        payload.put("per_page", Math.min(perPage, maxResults));

        if (request.keywords() != null && !request.keywords().isBlank()) {
            payload.put("q_keywords", request.keywords().trim());
        }
        if (request.titleKeywords() != null && !request.titleKeywords().isEmpty()) {
            payload.put("person_titles", request.titleKeywords().stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).toList());
        }
        if (request.locations() != null && !request.locations().isEmpty()) {
            payload.put("person_locations", request.locations().stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).toList());
        }

        if (overrides != null && overrides.extraFilters() != null && !overrides.extraFilters().isEmpty()) {
            payload.putAll(overrides.extraFilters());
        }

        return payload;
    }

    private List<QualifiedCandidateDto> toQualifiedCandidates(JsonNode response, ApolloCandidateSearchRequest request, boolean requireAllRequired) {
        List<JsonNode> candidates = extractCandidateNodes(response);
        List<QualifiedCandidateDto> qualified = new ArrayList<>();

        for (JsonNode node : candidates) {
            String fullName = text(node, "name", "full_name", "fullName");
            if (fullName == null || fullName.isBlank()) {
                String first = text(node, "first_name", "firstName");
                String last = text(node, "last_name", "lastName");
                fullName = (first == null ? "" : first) + (last == null ? "" : (" " + last));
                fullName = fullName.trim();
            }

            String title = text(node, "title", "headline", "job_title");
            String company = text(node, "organization_name", "company", "company_name");
            String location = text(node, "location", "city", "state", "country");
            String email = text(node, "email", "email_address", "emailAddress");
            String linkedinUrl = text(node, "linkedin_url", "linkedinUrl", "linkedin_profile_url");

            List<String> candidateSkills = extractStringList(node, "skills", "skill_names");

            String searchableText = String.join(" ",
                    safe(fullName),
                    safe(title),
                    safe(company),
                    safe(location),
                    safe(linkedinUrl),
                    safe(email),
                    String.join(" ", candidateSkills)
            );

            CandidateQualificationScorer.ScoreResult scoreResult = scorer.score(
                    searchableText,
                    candidateSkills,
                    request.requiredSkills(),
                    request.optionalSkills(),
                    request.titleKeywords(),
                    request.locations()
            );

            int requiredCount = countNonBlank(request.requiredSkills());
            if (requireAllRequired && requiredCount > 0 && scoreResult.matchedRequired().size() < requiredCount) {
                continue;
            }

            qualified.add(new QualifiedCandidateDto(
                    blankToNull(fullName),
                    blankToNull(title),
                    blankToNull(company),
                    blankToNull(location),
                    blankToNull(email),
                    blankToNull(linkedinUrl),
                    scoreResult.score(),
                    scoreResult.matchedRequired(),
                    scoreResult.matchedOptional(),
                    node
            ));
        }

        log.info("Apollo search returned {} candidate(s); qualified={}", candidates.size(), qualified.size());
        return qualified;
    }

    private static boolean hasRequiredSkills(List<String> requiredSkills) {
        if (requiredSkills == null) return false;
        return requiredSkills.stream().anyMatch(s -> s != null && !s.isBlank());
    }

    private static int countNonBlank(List<String> values) {
        if (values == null) return 0;
        int count = 0;
        for (String v : values) {
            if (v != null && !v.isBlank()) count++;
        }
        return count;
    }

    private static List<JsonNode> extractCandidateNodes(JsonNode response) {
        if (response == null || response.isNull()) return List.of();

        JsonNode[] possibleArrays = new JsonNode[] {
                response.get("people"),
                response.get("persons"),
                response.get("contacts"),
                response.get("results"),
                response.at("/data/people"),
                response.at("/data/persons"),
                response.at("/data/contacts"),
                response.at("/data/results")
        };

        for (JsonNode node : possibleArrays) {
            if (node != null && node.isArray()) {
                List<JsonNode> list = new ArrayList<>();
                node.forEach(list::add);
                return list;
            }
        }

        return List.of();
    }

    private static List<String> extractStringList(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode v = node.get(field);
            if (v == null || v.isNull()) continue;
            if (v.isArray()) {
                List<String> list = new ArrayList<>();
                v.forEach(item -> {
                    if (item != null && !item.isNull() && item.isTextual()) list.add(item.asText());
                    if (item != null && !item.isNull() && item.isObject()) {
                        String name = text(item, "name", "value");
                        if (name != null && !name.isBlank()) list.add(name);
                    }
                });
                return list;
            }
            if (v.isTextual()) {
                return List.of(v.asText());
            }
        }
        return List.of();
    }

    private static String text(JsonNode node, String... fields) {
        for (String field : fields) {
            if (node == null) continue;
            JsonNode v = node.get(field);
            if (v != null && !v.isNull() && v.isValueNode()) {
                String s = v.asText();
                if (s != null && !s.isBlank()) return s;
            }
        }
        return null;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
