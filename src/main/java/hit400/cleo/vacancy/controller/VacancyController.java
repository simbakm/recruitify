package hit400.cleo.vacancy.controller;

import hit400.cleo.vacancy.dtos.VacancyRequest;
import hit400.cleo.vacancy.dtos.VacancyResponse;
import hit400.cleo.vacancy.services.VacancyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/vacancies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VacancyController {

    private final VacancyService vacancyService;

    @PostMapping
    public Mono<ResponseEntity<VacancyResponse>> create(@RequestBody VacancyRequest request) {
        return vacancyService.create(request)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<VacancyResponse>> getById(@PathVariable Long id) {
        return vacancyService.getById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Flux<VacancyResponse> getAll(@RequestParam(required = false) Integer companyId) {
        return companyId != null
                ? vacancyService.getByCompanyId(companyId)
                : vacancyService.getAll();
    }

    @GetMapping("/recommended/{profileId}")
    public Flux<VacancyResponse> getRecommended(@PathVariable Long profileId) {
        return vacancyService.getRecommended(profileId);
    }

    @GetMapping("/{id}/close")
    public Mono<ResponseEntity<Void>> closeAndScore(@PathVariable Long id) {
        return vacancyService.closeAndScore(id, true)
                .thenReturn(ResponseEntity.ok().build());
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<VacancyResponse>> update(@PathVariable Long id, @RequestBody VacancyRequest request) {
        return vacancyService.update(id, request)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id) {
        return vacancyService.delete(id)
                .thenReturn(ResponseEntity.noContent().build());
    }
}

