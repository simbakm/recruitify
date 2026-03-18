package hit400.cleo.candidate.services;

import hit400.cleo.candidate.dtos.ApplicationRequest;
import hit400.cleo.candidate.dtos.ApplicationResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApplicationService {

    Mono<ApplicationResponse> create(ApplicationRequest request);

    Mono<ApplicationResponse> getById(Long id);

    Flux<ApplicationResponse> getAll(Long vacancyId, Long candidateId, String status);

    Mono<ApplicationResponse> update(Long id, ApplicationRequest request);

    Mono<Void> delete(Long id);
}
