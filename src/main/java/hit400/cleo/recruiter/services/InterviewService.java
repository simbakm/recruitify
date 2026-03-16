package hit400.cleo.recruiter.services;

import hit400.cleo.recruiter.dtos.InterviewRequest;
import hit400.cleo.recruiter.dtos.InterviewResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface InterviewService {

    Mono<InterviewResponse> create(InterviewRequest request);

    Mono<InterviewResponse> getById(Long id);

    Flux<InterviewResponse> getAll();

    Mono<InterviewResponse> update(Long id, InterviewRequest request);

    Mono<Void> delete(Long id);
}

