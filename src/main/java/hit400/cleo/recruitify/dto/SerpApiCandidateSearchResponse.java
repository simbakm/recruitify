package hit400.cleo.recruitify.dto;

import java.util.List;

public record SerpApiCandidateSearchResponse(
        List<QualifiedCandidateDto> candidates
) {
}

