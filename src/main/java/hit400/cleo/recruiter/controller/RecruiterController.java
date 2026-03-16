package hit400.cleo.recruiter.controller;

import hit400.cleo.recruiter.dtos.RecruiterRequest;
import hit400.cleo.recruiter.dtos.RecruiterResponse;
import hit400.cleo.recruiter.services.RecruiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/recruiters")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecruiterController {

    private final RecruiterService recruiterService;

    @PostMapping
    public Mono<ResponseEntity<RecruiterResponse>> create(@RequestBody RecruiterRequest request) {
        return recruiterService.create(request)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<RecruiterResponse>> getById(@PathVariable Long id) {
        return recruiterService.getById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Flux<RecruiterResponse> getAll() {
        return recruiterService.getAll();
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<RecruiterResponse>> update(@PathVariable Long id, @RequestBody RecruiterRequest request) {
        return recruiterService.update(id, request)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return recruiterService.delete(id)
                .thenReturn(ResponseEntity.noContent().build());
    }
}

