package hit400.cleo.recruitify.dto;

import java.util.List;
import java.util.Map;

public record ApolloCandidateSearchRequest(
        String keywords,
        List<String> requiredSkills,
        List<String> optionalSkills,
        List<String> titleKeywords,
        List<String> locations,
        Integer minYearsExperience,
        Boolean requireAllRequiredSkills,
        Integer maxResults,
        ApolloOverrides apollo
) {
    public record ApolloOverrides(
            Integer page,
            Integer perPage,
            Map<String, Object> extraFilters
    ) {}
}

