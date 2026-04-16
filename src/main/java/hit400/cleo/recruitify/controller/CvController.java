package hit400.cleo.recruitify.controller;

import hit400.cleo.recruitify.dto.CandidateProfileResponseDto;
import hit400.cleo.recruitify.service.CvExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.core.io.Resource;


@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("api/cv")

@RequiredArgsConstructor
public class CvController {
    private final CvExtractionService extractionService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<CandidateProfileResponseDto>> uploadCV(
            @RequestParam("profileId") Long profileId,
            @RequestPart("cvFile") FilePart cvFilePart) {
        if (profileId == null || cvFilePart == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return extractionService.extractAndUpdateProfile(profileId, cvFilePart)
                .map(result -> {
                    var profile = result.profile();
                    String objectives = profile.getObjectives();

                    return ResponseEntity.ok(new CandidateProfileResponseDto(
                            profile.getId(),
                            profile.getName(),
                            profile.getEmail(),
                            profile.getPhone(),
                            profile.getAddress(),
                            profile.getExperiences().stream().map(e -> new CandidateProfileResponseDto.ExperienceDto(
                                    e.getJobTitle(),
                                    e.getCompany(),
                                    e.getStartDate(),
                                    e.getEndDate(),
                                    e.getDescription()
                            )).toList(),
                            profile.getEducations().stream().map(e -> new CandidateProfileResponseDto.EducationDto(
                                    e.getDegree(),
                                    e.getInstitution(),
                                    e.getGraduationYear(),
                                    e.getDescription()
                            )).toList(),
                            profile.getSkills(),
                            objectives,  // Add the objectives field
                            profile.getCreatedAt(),
                            result.warnings()
                    ));
                });
    }

    @GetMapping("/view/{id}")
    @Operation(
            summary = "View a CV as PDF",
            description = "Retrieves the CV file associated with a specific candidate profile ID and returns it as a PDF for viewing."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CV found and returned successfully",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "404", description = "Profile not found or CV file missing",
                    content = @Content)
    })
    public Mono<ResponseEntity<Resource>> viewCv(
            @Parameter(description = "ID of the candidate profile whose CV is to be viewed", example = "123", required = true)
            @PathVariable Long id) {
        return extractionService.getCvFile(id)
                .map(resource -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(resource))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build())); // Optional: handle missing case
    }
}
