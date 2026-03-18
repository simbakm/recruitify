package hit400.cleo.recruitify.dto;
 
import java.util.List;
import java.util.Map;
 
public record LeadIQCandidateSearchRequest(
        String keywords,
        List<String> requiredSkills,
        List<String> optionalSkills,
        List<String> titleKeywords,
        List<String> locations,
        Integer minYearsExperience,
        Boolean requireAllRequiredSkills,
        Integer maxResults,
        LeadIQOverrides leadiq
) {
    public record LeadIQOverrides(
            Integer skip,
            Integer limit,
            Map<String, Object> extraFilters
    ) {}
}
