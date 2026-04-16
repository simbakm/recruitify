package hit400.cleo.recruitify.service.serpapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import hit400.cleo.recruitify.dto.QualifiedCandidateDto;
import hit400.cleo.recruitify.dto.SerpApiCandidateSearchRequest;
import hit400.cleo.recruitify.service.snovio.SnovIoEmailFinderService;
import hit400.cleo.recruitify.service.util.CandidateQualificationScorer;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SerpApiCandidateSearchService {

    private static final Logger log = LogManager.getLogger(SerpApiCandidateSearchService.class);

    private final WebClient serpApiWebClient;
    private final SerpApiProperties properties;
    private final ObjectMapper objectMapper;
    private final SnovIoEmailFinderService snovIoEmailFinderService;

    private final CandidateQualificationScorer scorer = new CandidateQualificationScorer();

    public Mono<List<QualifiedCandidateDto>> searchQualifiedCandidates(String position, List<String> skills) {
        return searchQualifiedCandidates(new SerpApiCandidateSearchRequest(
                position,
                skills,
                List.of(),
                null,
                false,
                null,
                null,
                null,
                new SerpApiCandidateSearchRequest.SerpApiOverrides(null, null, null, null)
        ));
    }

    public Mono<List<QualifiedCandidateDto>> searchQualifiedCandidates(SerpApiCandidateSearchRequest request) {
        if (request == null) {
            return Mono.error(new IllegalArgumentException("Request body is required"));
        }
        if (request.position() == null || request.position().isBlank()) {
            return Mono.error(new IllegalArgumentException("position is required"));
        }
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            return Mono.error(new IllegalStateException("SerpAPI key is missing. Set serpapi.api-key (or SERPAPI_API_KEY)."));
        }

        int limit = clampLimit(request.maxResults(), properties.defaultLimit(), properties.minLimit());
        String site = (request.serpapi() != null && request.serpapi().site() != null) ? request.serpapi().site() : properties.defaultSite();
        Integer start = (request.serpapi() != null) ? request.serpapi().start() : null;

        List<String> locationKeywords = normalizeList(request.locations());
        String q = buildQuery(request.position(), request.requiredSkills(), request.optionalSkills(), locationKeywords, site);

        return serpApiWebClient
                .get()
                .uri(uriBuilder -> {
                    var b = uriBuilder
                            .path("/search.json")
                            .queryParam("engine", properties.engine())
                            .queryParam("q", q)
                            .queryParam("api_key", properties.apiKey())
                            .queryParam("num", limit)
                            .queryParam("safe", properties.safe());

                    if (properties.googleDomain() != null) b = b.queryParam("google_domain", properties.googleDomain());
                    if (!locationKeywords.isEmpty()) {
                        b = b.queryParam("location", locationKeywords.get(0));
                    } else if (properties.location() != null) {
                        b = b.queryParam("location", properties.location());
                    }
                    if (properties.hl() != null) b = b.queryParam("hl", properties.hl());
                    if (properties.gl() != null) b = b.queryParam("gl", properties.gl());
                    if (start != null && start >= 0) b = b.queryParam("start", start);

                    if (request.serpapi() != null && request.serpapi().hl() != null && !request.serpapi().hl().isBlank()) {
                        b = b.queryParam("hl", request.serpapi().hl());
                    }
                    if (request.serpapi() != null && request.serpapi().gl() != null && !request.serpapi().gl().isBlank()) {
                        b = b.queryParam("gl", request.serpapi().gl());
                    }
                    return b.build();
                })
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new SerpApiException(response.statusCode(), body)))
                )
                .bodyToMono(String.class)
                .flatMap(body -> Mono.fromCallable(() -> objectMapper.readTree(body)))
                .flatMap(json -> {
                    JsonNode error = json.get("error");
                    if (error != null && !error.isNull() && !error.asText("").isBlank()) {
                        return Mono.error(new RuntimeException("SerpAPI error: " + error.asText("")));
                    }
                    return Mono.just(json);
                })
                .map(json -> toQualifiedCandidates(json, request))
                .map(list -> list.stream()
                        .sorted(Comparator.comparingDouble(QualifiedCandidateDto::matchScore).reversed())
                        .limit(limit)
                        .toList())
                .flatMap(list -> maybeEnrichEmails(list, request))
                .doOnSuccess(list -> log.info("SerpAPI search completed: position='{}' results={}", request.position(), list.size()))
                .doOnError(error -> log.error("SerpAPI search failed: position='{}'", request.position(), error));
    }

    private Mono<List<QualifiedCandidateDto>> maybeEnrichEmails(List<QualifiedCandidateDto> candidates, SerpApiCandidateSearchRequest request) {
        if (candidates == null || candidates.isEmpty()) return Mono.just(List.of());
        if (request.includeEmails() == null || !request.includeEmails()) return Mono.just(candidates);
        if (request.emailDomain() == null || request.emailDomain().isBlank()) return Mono.just(candidates);

        List<SnovIoEmailFinderService.NameDomainRow> rows = candidates.stream()
                .map(c -> toRow(c, request.emailDomain()))
                .filter(r -> r != null)
                .toList();

        if (rows.isEmpty()) return Mono.just(candidates);

        return snovIoEmailFinderService.findEmailsByNameAndDomain(rows)
                .map(emailByPerson -> candidates.stream()
                        .map(c -> {
                            if (c.fullName() == null || c.fullName().isBlank()) return c;
                            String key = c.fullName().trim().toLowerCase(Locale.ROOT);
                            String email = emailByPerson.get(key);
                            if (email == null || email.isBlank()) return c;
                            return new QualifiedCandidateDto(
                                    c.fullName(),
                                    c.title(),
                                    c.company(),
                                    c.location(),
                                    email,
                                    c.linkedinUrl(),
                                    c.matchScore(),
                                    c.matchedRequiredSkills(),
                                    c.matchedOptionalSkills(),
                                    c.source()
                            );
                        })
                        .toList());
    }

    private static SnovIoEmailFinderService.NameDomainRow toRow(QualifiedCandidateDto candidate, String domain) {
        if (candidate == null) return null;
        if (candidate.fullName() == null || candidate.fullName().isBlank()) return null;
        if (domain == null || domain.isBlank()) return null;

        String[] parts = candidate.fullName().trim().split("\\s+");
        if (parts.length < 2) return null;

        String firstName = parts[0].trim();
        String lastName = parts[parts.length - 1].trim();
        if (firstName.isBlank() || lastName.isBlank()) return null;

        return new SnovIoEmailFinderService.NameDomainRow(firstName, lastName, domain.trim());
    }

    private List<QualifiedCandidateDto> toQualifiedCandidates(JsonNode response, SerpApiCandidateSearchRequest request) {
        JsonNode organic = response.get("organic_results");
        if (organic == null || organic.isNull() || !organic.isArray() || organic.isEmpty()) {
            return List.of();
        }

        List<String> required = normalizeList(request.requiredSkills());
        List<String> optional = normalizeList(request.optionalSkills());
        List<String> titleKeywords = List.of(request.position());
        List<String> locations = normalizeList(request.locations());

        boolean requireAllRequired = request.requireAllRequiredSkills() != null && request.requireAllRequiredSkills();
        int requiredCount = required.size();

        List<QualifiedCandidateDto> out = new ArrayList<>();
        for (JsonNode r : organic) {
            String title = r.path("title").asText("");
            String link = r.path("link").asText("");
            String snippet = r.path("snippet").asText("");
            String displayedLink = r.path("displayed_link").asText("");

            String searchableText = String.join(" ", title, snippet, displayedLink, link);

            CandidateQualificationScorer.ScoreResult score = scorer.score(
                    searchableText,
                    List.of(),
                    required,
                    optional,
                    titleKeywords,
                    locations
            );

            if (requireAllRequired && requiredCount > 0 && score.matchedRequired().size() < requiredCount) {
                continue;
            }

            String linkedinUrl = link.toLowerCase(Locale.ROOT).contains("linkedin.com") ? link : null;
            String fullName = extractLikelyFullName(title);

            out.add(new QualifiedCandidateDto(
                    fullName,
                    title.isBlank() ? null : title,
                    null,
                    null,
                    null,
                    linkedinUrl,
                    score.score(),
                    score.matchedRequired(),
                    score.matchedOptional(),
                    objectMapper.convertValue(r, Object.class)
            ));
        }

        return out;
    }

    static String extractLikelyFullName(String serpTitle) {
        if (serpTitle == null) return null;
        String t = serpTitle.trim();
        if (t.isBlank()) return null;

        // Common LinkedIn SERP formats:
        // "Jane Doe - Receptionist - Company | LinkedIn"
        // "John Smith - Software Engineer | LinkedIn"
        String namePart = t;
        int pipeIdx = namePart.indexOf('|');
        if (pipeIdx > 0) namePart = namePart.substring(0, pipeIdx).trim();
        namePart = namePart.replace("LinkedIn", "").trim();
        int dashIdx = namePart.indexOf(" - ");
        if (dashIdx > 0) namePart = namePart.substring(0, dashIdx).trim();

        // Very basic sanity check: at least two words, only letters/space/dot/hyphen/apostrophe
        if (!namePart.matches("[A-Za-z][A-Za-z .'-]+")) return null;
        String[] parts = namePart.split("\\s+");
        if (parts.length < 2) return null;
        if (namePart.length() > 70) return null;
        return namePart;
    }

    static String buildQuery(String position, List<String> requiredSkills, List<String> optionalSkills, List<String> locations, String site) {
        List<String> req = normalizeList(requiredSkills);
        List<String> opt = normalizeList(optionalSkills);
        List<String> loc = normalizeList(locations);

        StringBuilder sb = new StringBuilder();
        sb.append('"').append(position.trim()).append('"');

        if (!req.isEmpty()) {
            sb.append(' ').append('(');
            for (int i = 0; i < req.size(); i++) {
                if (i > 0) sb.append(" OR ");
                sb.append('"').append(req.get(i)).append('"');
            }
            sb.append(')');
        }

        if (!opt.isEmpty()) {
            sb.append(' ').append('(');
            for (int i = 0; i < opt.size(); i++) {
                if (i > 0) sb.append(" OR ");
                sb.append('"').append(opt.get(i)).append('"');
            }
            sb.append(')');
        }

        if (!loc.isEmpty()) {
            sb.append(' ').append('(');
            for (int i = 0; i < loc.size(); i++) {
                if (i > 0) sb.append(" OR ");
                sb.append('"').append(loc.get(i)).append('"');
            }
            sb.append(')');
        }

        if (site != null && !site.isBlank()) {
            sb.append(" site:").append(site.trim());
        }

        return sb.toString();
    }

    static int clampLimit(Integer requested, int defaultLimit, int minLimit) {
        int raw = requested == null ? defaultLimit : requested;
        int lowerBound = Math.max(1, minLimit);
        return Math.max(lowerBound, Math.min(100, raw));
    }

    private static List<String> normalizeList(List<String> list) {
        if (list == null || list.isEmpty()) return List.of();
        return list.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
