package hit400.cleo.recruitify.dto;

import java.util.List;

public record SerpApiCandidateSearchRequest(
        String position,
        List<String> requiredSkills,
        List<String> optionalSkills,
        List<String> locations,
        Boolean includeEmails,
        String emailDomain,
        Boolean requireAllRequiredSkills,
        Integer maxResults,
        SerpApiOverrides serpapi
) {
    public record SerpApiOverrides(
            String site,
            Integer start,
            String hl,
            String gl
    ) {}

    public SerpApiCandidateSearchRequest {
        if (optionalSkills == null) optionalSkills = List.of();
        if (includeEmails == null) includeEmails = false;
    }
}
