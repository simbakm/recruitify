package hit400.cleo.vacancy.services;

import hit400.cleo.vacancy.dtos.VacancyRequest;
import hit400.cleo.vacancy.dtos.VacancyResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface VacancyService {

    Mono<VacancyResponse> create(VacancyRequest request);

    Mono<VacancyResponse> getById(Long id);

    Flux<VacancyResponse> getAll();

    Mono<VacancyResponse> update(Long id, VacancyRequest request);

    Mono<Void> delete(Long id);
}

