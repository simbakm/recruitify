package hit400.cleo.recruitify.controller;

import hit400.cleo.recruitify.dto.CandidateProfileDto;
import hit400.cleo.recruitify.dto.CvUploadRequest;
import hit400.cleo.recruitify.service.CvExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("api/cv")

@RequiredArgsConstructor
public class CvController {
    private final CvExtractionService extractionService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<CandidateProfileDto>> uploadCV(
            @RequestPart("cvFile") FilePart cvFilePart) {

        CvUploadRequest request = new CvUploadRequest(cvFilePart);

        return extractionService.extractAndSaveProfile(request)
                .map(profile -> {
                    // Extract objectives if available (you might need to add this field to your entity)
                    String objectives = null; // You might want to add an objectives field to CandidateProfile

                    return ResponseEntity.ok(new CandidateProfileDto(
                            profile.getId(),
                            profile.getName(),
                            profile.getEmail(),
                            profile.getPhone(),
                            profile.getAddress(),
                            profile.getExperiences().stream().map(e -> new CandidateProfileDto.ExperienceDto(
                                    e.getJobTitle(),
                                    e.getCompany(),
                                    e.getStartDate(),
                                    e.getEndDate(),
                                    e.getDescription()
                            )).toList(),
                            profile.getEducations().stream().map(e -> new CandidateProfileDto.EducationDto(
                                    e.getDegree(),
                                    e.getInstitution(),
                                    e.getGraduationYear(),
                                    e.getDescription()
                            )).toList(),
                            profile.getSkills(),
                            objectives,  // Add the objectives field
                            profile.getCreatedAt()
                    ));
                });
    }
}