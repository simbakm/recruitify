package hit400.cleo.candidate.controller;

import hit400.cleo.candidate.dtos.InterviewRequest;
import hit400.cleo.candidate.dtos.InterviewResponse;
import hit400.cleo.candidate.services.CandidateInterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/candidate/interviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CandidateInterviewController {

    private final CandidateInterviewService interviewService;

    @PostMapping
    public Mono<ResponseEntity<InterviewResponse>> create(@RequestBody InterviewRequest request) {
        return interviewService.create(request)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<InterviewResponse>> getById(@PathVariable Long id) {
        return interviewService.getById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Flux<InterviewResponse> getAll() {
        return interviewService.getAll();
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<InterviewResponse>> update(@PathVariable Long id, @RequestBody InterviewRequest request) {
        return interviewService.update(id, request)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return interviewService.delete(id)
                .thenReturn(ResponseEntity.noContent().build());
    }
}

