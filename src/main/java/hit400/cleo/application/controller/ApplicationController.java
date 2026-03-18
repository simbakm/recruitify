package hit400.cleo.application.controller;

import hit400.cleo.application.dtos.ApplicationRequest;
import hit400.cleo.application.dtos.ApplicationResponse;
import hit400.cleo.application.dtos.ApplicationStatusUpdateRequest;
import hit400.cleo.application.services.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApplicationController {

    private final ApplicationService applicationService;

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

    @PutMapping("/{id}/status")
    public Mono<ApplicationResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody ApplicationStatusUpdateRequest request) {
        return applicationService.updateStatus(id, request);
    }
}
