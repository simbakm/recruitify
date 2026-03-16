package hit400.cleo.recruiter.services;

import hit400.cleo.recruiter.dtos.RecruiterRequest;
import hit400.cleo.recruiter.dtos.RecruiterResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RecruiterService {

    Mono<RecruiterResponse> create(RecruiterRequest request);

    Mono<RecruiterResponse> getById(Long id);

    Flux<RecruiterResponse> getAll();

    Mono<RecruiterResponse> update(Long id, RecruiterRequest request);

    Mono<Void> delete(Long id);
}

