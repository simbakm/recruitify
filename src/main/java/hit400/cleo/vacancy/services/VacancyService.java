package hit400.cleo.vacancy.services;

import hit400.cleo.vacancy.dtos.VacancyRequest;
import hit400.cleo.vacancy.dtos.VacancyResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface VacancyService {

    Mono<VacancyResponse> create(VacancyRequest request);

    Mono<VacancyResponse> getById(Long id);

    Flux<VacancyResponse> getAll();

    Flux<VacancyResponse> getByCompanyId(Integer companyId);

    Flux<VacancyResponse> getRecommended(Long profileId);

    Mono<Void> closeAndScore(Long vacancyId, boolean forceClose);

    Mono<VacancyResponse> update(Long id, VacancyRequest request);

    Mono<Void> delete(Long id);
}

