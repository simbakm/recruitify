package hit400.cleo.recruitify.dto;

import hit400.cleo.recruitify.model.CandidateProfile;

import java.util.List;

public record CandidateProfileRequestDTO(
            String name,
            String email,
            String phone,
            String address,
            String objectives,
            Boolean lookingForJob,
            String desiredJobTitle,
            String desiredCategory,
            String preferredWorkMode,
            String preferredLocation,
            Integer salaryMin,
            Integer salaryMax,
            String salaryCurrency,
            List<String> skills,
            List<CandidateProfile.Experience> experiences,
            List<CandidateProfile.Education> educations
    ) {}

