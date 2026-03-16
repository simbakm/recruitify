package hit400.cleo.candidate.services;

import hit400.cleo.candidate.dtos.InterviewRequest;
import hit400.cleo.candidate.dtos.InterviewResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CandidateInterviewService {

    Mono<InterviewResponse> create(InterviewRequest request);

    Mono<InterviewResponse> getById(Long id);

    Flux<InterviewResponse> getAll();

    Mono<InterviewResponse> update(Long id, InterviewRequest request);

    Mono<Void> delete(Long id);
}

