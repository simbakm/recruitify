package hit400.cleo.application.services;

import hit400.cleo.application.dtos.ApplicationRequest;
import hit400.cleo.application.dtos.ApplicationResponse;
import hit400.cleo.application.dtos.ApplicationStatusUpdateRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApplicationService {

    Mono<ApplicationResponse> create(Long vacancyId, Long candidateId, ApplicationRequest request);

    Flux<ApplicationResponse> getByCandidateId(Long candidateId);

    Flux<ApplicationResponse> getByVacancyId(Long vacancyId);

    Mono<ApplicationResponse> updateStatus(Long applicationId, ApplicationStatusUpdateRequest request);
}
