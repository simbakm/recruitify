package hit400.cleo.candidate.controller;

import hit400.cleo.candidate.dtos.ApplicationRequest;
import hit400.cleo.candidate.dtos.ApplicationResponse;
import hit400.cleo.candidate.services.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    public Mono<ResponseEntity<ApplicationResponse>> create(@RequestBody ApplicationRequest request) {
        return applicationService.create(request)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApplicationResponse>> getById(@PathVariable Long id) {
        return applicationService.getById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Flux<ApplicationResponse> getAll(
            @RequestParam(required = false) Long vacancyId,
            @RequestParam(required = false) Long candidateId,
            @RequestParam(required = false) String status
    ) {
        return applicationService.getAll(vacancyId, candidateId, status);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApplicationResponse>> update(
            @PathVariable Long id,
            @RequestBody ApplicationRequest request
    ) {
        return applicationService.update(id, request)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return applicationService.delete(id).thenReturn(ResponseEntity.noContent().build());
    }
}

