package hit400.cleo.recruitify.dto;

import java.util.List;

public record ApolloCandidateSearchResponse(
        List<QualifiedCandidateDto> candidates
) {
}

