package hit400.cleo.recruitify.dto;
 
import java.util.List;
 
public record LeadIQCandidateSearchResponse(
        List<QualifiedCandidateDto> candidates
) {
}
