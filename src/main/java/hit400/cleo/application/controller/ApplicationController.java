package hit400.cleo.application.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hit400.cleo.application.dtos.ApplicationRequest;
import hit400.cleo.application.dtos.ApplicationResponse;
import hit400.cleo.application.dtos.ApplicationStatusUpdateRequest;
import hit400.cleo.application.services.ApplicationService;
import hit400.cleo.recruitify.dto.SerpApiCandidateSearchRequest;
import hit400.cleo.recruitify.dto.SerpApiCandidateSearchResponse;
import hit400.cleo.recruitify.service.serpapi.SerpApiCandidateSearchService;
import hit400.cleo.vacancy.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApplicationController {

    private final ApplicationService applicationService ;
    private final VacancyRepository vacancyRepository;
    private final SerpApiCandidateSearchService serpApiCandidateSearchService;


    @PostMapping("/vacancies/{vacancyId}/candidates/{candidateId}")
    public Mono<ResponseEntity<ApplicationResponse>> create(
            @PathVariable Long vacancyId,
            @PathVariable Long candidateId,
            @RequestBody ApplicationRequest request) {
        return applicationService.create(vacancyId, candidateId, request)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved));
    }

    @PostMapping(value = "/vacancies/{vacancyId}/candidates/{candidateId}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Mono<ResponseEntity<ApplicationResponse>> createPlainText(
            @PathVariable Long vacancyId,
            @PathVariable Long candidateId,
            @RequestBody String coverLetter) {
        ApplicationRequest request = ApplicationRequest.builder()
                .coverLetter(coverLetter)
                .build();
        return applicationService.create(vacancyId, candidateId, request)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved));
    }

    @GetMapping("/candidate/{candidateId}")
    public Flux<ApplicationResponse> getByCandidate(@PathVariable Long candidateId) {
        return applicationService.getByCandidateId(candidateId);
    }

    @GetMapping("/vacancy/{vacancyId}")
    public Flux<ApplicationResponse> getByVacancy(@PathVariable Long vacancyId) {
        return applicationService.getByVacancyId(vacancyId);
    }

    /**
     * Uses the selected vacancy's information (title + requirements + location) to fetch recommended candidates from SerpAPI.
     * This is designed for the applicants view (HR selects a vacancy, then clicks "Get recommended candidates").
     */
    @GetMapping("/vacancy/{vacancyId}/recommended-candidates")
    public Mono<ResponseEntity<SerpApiCandidateSearchResponse>> getRecommendedCandidates(
            @PathVariable Long vacancyId,
            @RequestParam(required = false) Integer maxResults,
            @RequestParam(required = false) Boolean requireAllRequiredSkills
    ) {
        return vacancyRepository.findById(vacancyId)
                .flatMap(vacancy -> {
                    List<String> requirements = vacancy.getRequirements() == null ? List.of() : vacancy.getRequirements();
                    List<String> locations = (vacancy.getLocation() == null || vacancy.getLocation().isBlank())
                            ? null
                            : List.of(vacancy.getLocation());

                    SerpApiCandidateSearchRequest request = new SerpApiCandidateSearchRequest(
                            vacancy.getTitle(),
                            requirements,
                            List.of(),
                            locations,
                            false,
                            null,
                            requireAllRequiredSkills,
                            maxResults,
                            null
                    );

                    return serpApiCandidateSearchService.searchQualifiedCandidates(request)
                            .map(candidates -> ResponseEntity.ok(new SerpApiCandidateSearchResponse(candidates)));
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public Mono<ApplicationResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody ApplicationStatusUpdateRequest request) {
        return applicationService.updateStatus(id, request);
    }
}
