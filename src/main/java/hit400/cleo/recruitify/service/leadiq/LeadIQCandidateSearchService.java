package hit400.cleo.recruitify.service.leadiq;
 
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hit400.cleo.recruitify.dto.LeadIQCandidateSearchRequest;
import hit400.cleo.recruitify.dto.QualifiedCandidateDto;
import hit400.cleo.recruitify.service.util.CandidateQualificationScorer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
 
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
 
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadIQCandidateSearchService {
 
    private final WebClient leadiqWebClient;
    private final LeadIQProperties properties;
    private final ObjectMapper objectMapper;
 
    private final CandidateQualificationScorer scorer = new CandidateQualificationScorer();
 
    public Mono<List<QualifiedCandidateDto>> searchQualifiedCandidates(LeadIQCandidateSearchRequest request) {
        if (request == null) {
            return Mono.error(new IllegalArgumentException("Request body is required"));
        }
 
        int limit = request.maxResults() == null ? properties.defaultLimit() : Math.max(1, Math.min(100, request.maxResults()));
        int skip = (request.leadiq() != null && request.leadiq().skip() != null) ? request.leadiq().skip() : 0;
 
        Map<String, Object> variables = buildVariables(request, limit, skip);
        String query = """
                query FlatAdvancedSearch($input: FlatSearchInput!) {
                  flatAdvancedSearch(input: $input) {
                    totalPeople
                    people {
                      id
                      name
                      firstName
                      lastName
                      title
                      linkedinUrl
                      city
                      state
                      country
                      personalEmails
                      workEmails
                      verifiedWorkEmails
                      verifiedLikelyWorkEmails
                      company {
                        name
                      }
                    }
                  }
                }
                """;
 
        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("variables", Map.of("input", variables));
 
        return leadiqWebClient
                .post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(responseBody -> Mono.error(new LeadIQApiException(response.statusCode(), responseBody)))
                )
                .bodyToMono(JsonNode.class)
                .map(json -> toQualifiedCandidates(json, request))
                .map(list -> list.stream()
                        .sorted(Comparator.comparingDouble(QualifiedCandidateDto::matchScore).reversed())
                        .limit(limit)
                        .toList());
    }
 
    private Map<String, Object> buildVariables(LeadIQCandidateSearchRequest request, int limit, int skip) {
        Map<String, Object> input = new HashMap<>();
        input.put("limit", limit);
        input.put("skip", skip);
 
        Map<String, Object> contactFilter = new HashMap<>();
        if (request.titleKeywords() != null && !request.titleKeywords().isEmpty()) {
            contactFilter.put("titles", request.titleKeywords());
        }
        if (request.locations() != null && !request.locations().isEmpty()) {
            // LeadIQ might expect structured locations, but for now we pass them as strings if the API supports it
            // Or we could map them to LocationFilterInput if we had more details.
            // Using a simple list of strings for now as a fallback.
            contactFilter.put("locations", request.locations().stream().map(loc -> Map.of("city", loc)).toList());
        }
 
        if (request.leadiq() != null && request.leadiq().extraFilters() != null) {
            contactFilter.putAll(request.leadiq().extraFilters());
        }
 
        input.put("contactFilter", contactFilter);
        return input;
    }
 
    private List<QualifiedCandidateDto> toQualifiedCandidates(JsonNode response, LeadIQCandidateSearchRequest request) {
        JsonNode data = response.get("data");
        if (data == null || data.isNull()) {
            log.warn("LeadIQ response has no data: {}", response);
            return List.of();
        }
 
        JsonNode searchResult = data.get("flatAdvancedSearch");
        if (searchResult == null || searchResult.isNull()) {
            return List.of();
        }
 
        JsonNode results = searchResult.get("people");
        List<QualifiedCandidateDto> qualified = new ArrayList<>();
 
        if (results != null && results.isArray()) {
            for (JsonNode node : results) {
                String fullName = node.path("name").asText("");
                if (fullName.isBlank()) {
                    String first = node.path("firstName").asText("");
                    String last = node.path("lastName").asText("");
                    fullName = (first + " " + last).trim();
                }
 
                String title = node.path("title").asText("");
                String company = node.at("/company/name").asText("");
 
                String location = formatLocation(node);
                String linkedinUrl = node.path("linkedinUrl").asText("");
 
                // Emails
                String email = firstEmail(node);
 
                String searchableText = String.join(" ", fullName, title, company, location, linkedinUrl, email);
 
                CandidateQualificationScorer.ScoreResult scoreResult = scorer.score(
                        searchableText,
                        List.of(), // skills might be missing in basic search
                        request.requiredSkills(),
                        request.optionalSkills(),
                        request.titleKeywords(),
                        request.locations()
                );
 
                boolean requireAllRequired = request.requireAllRequiredSkills() != null && request.requireAllRequiredSkills();
                int requiredCount = request.requiredSkills() == null ? 0 : (int) request.requiredSkills().stream().filter(s -> s != null && !s.isBlank()).count();
 
                if (requireAllRequired && requiredCount > 0 && scoreResult.matchedRequired().size() < requiredCount) {
                    continue;
                }
 
                qualified.add(new QualifiedCandidateDto(
                        fullName.isBlank() ? null : fullName,
                        title.isBlank() ? null : title,
                        company.isBlank() ? null : company,
                        location.isBlank() ? null : location,
                        email.isBlank() ? null : email,
                        linkedinUrl.isBlank() ? null : linkedinUrl,
                        scoreResult.score(),
                        scoreResult.matchedRequired(),
                        scoreResult.matchedOptional(),
                        node
                ));
            }
        }
 
        return qualified;
    }

    private static String formatLocation(JsonNode node) {
        String city = node.path("city").asText("");
        String state = node.path("state").asText("");
        String country = node.path("country").asText("");

        List<String> parts = new ArrayList<>(3);
        if (!city.isBlank()) parts.add(city);
        if (!state.isBlank()) parts.add(state);
        if (!country.isBlank()) parts.add(country);
        return String.join(", ", parts);
    }

    private static String firstEmail(JsonNode node) {
        // Prefer verified work emails if present, then likely verified, then other work, then personal.
        String email =
                firstStringFromArray(node.path("verifiedWorkEmails"));
        if (email.isBlank()) email = firstStringFromArray(node.path("verifiedLikelyWorkEmails"));
        if (email.isBlank()) email = firstStringFromArray(node.path("workEmails"));
        if (email.isBlank()) email = firstStringFromArray(node.path("personalEmails"));
        return email;
    }

    private static String firstStringFromArray(JsonNode maybeArray) {
        if (maybeArray == null || maybeArray.isNull() || !maybeArray.isArray() || maybeArray.isEmpty()) {
            return "";
        }
        JsonNode first = maybeArray.get(0);
        if (first == null || first.isNull()) {
            return "";
        }
        // Some endpoints/models may represent records as objects like { value: "..." }.
        if (first.isObject()) {
            return first.path("value").asText("");
        }
        return first.asText("");
    }
}
