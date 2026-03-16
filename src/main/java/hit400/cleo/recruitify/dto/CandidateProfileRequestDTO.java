package hit400.cleo.recruitify.dto;

import hit400.cleo.recruitify.model.CandidateProfile;

import java.util.List;

public record CandidateProfileRequestDTO(
            String name,
            String phone,
            String address,
            String objectives,
            List<String> skills,
            List<CandidateProfile.Experience> experiences,
            List<CandidateProfile.Education> educations
    ) {}

