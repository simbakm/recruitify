package hit400.cleo.recruitify.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CandidateProfileResponseDto(
        Long id,
        String name,
        String email,
        String phone,
        String address,
        List<ExperienceDto> experiences,
        List<EducationDto> educations,
        List<String> skills,
        String objectives,  // Added objectives field
        LocalDateTime createdAt,
        List<String> warnings
) {
    public record ExperienceDto(
            String jobTitle,
            String company,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String description
    ) {}

    public record EducationDto(
            String degree,
            String institution,
            LocalDateTime graduationYear,
            String description
    ) {}
}
