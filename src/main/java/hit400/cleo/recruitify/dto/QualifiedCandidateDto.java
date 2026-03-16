package hit400.cleo.recruitify.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record QualifiedCandidateDto(
        String fullName,
        String title,
        String company,
        String location,
        String email,
        String linkedinUrl,
        double matchScore,
        List<String> matchedRequiredSkills,
        List<String> matchedOptionalSkills,
        JsonNode source
) {
}

