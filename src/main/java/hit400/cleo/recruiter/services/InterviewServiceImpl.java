package hit400.cleo.recruiter.services;

import hit400.cleo.recruiter.dtos.InterviewRequest;
import hit400.cleo.recruiter.dtos.InterviewResponse;
import hit400.cleo.recruiter.model.Interview;
import hit400.cleo.recruiter.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private static final Logger log = LogManager.getLogger(InterviewServiceImpl.class);

    private final InterviewRepository interviewRepository;

    @Override
    public Mono<InterviewResponse> create(InterviewRequest request) {
        log.info("Creating interview");
        Interview interview = applyRequest(request, new Interview());
        return interviewRepository.save(interview)
                .map(this::toResponse)
                .doOnSuccess(saved -> log.info("Saved successfully: interview id={}", saved.getId()))
                .doOnError(error -> log.error("Failed to create interview", error));
    }

    @Override
    public Mono<InterviewResponse> getById(Long id) {
        log.info("Fetching interview: id={}", id);
        return interviewRepository.findById(id).map(this::toResponse);
    }

    @Override
    public Flux<InterviewResponse> getAll() {
        log.info("Fetching all interviews");
        return interviewRepository.findAll().map(this::toResponse);
    }

    @Override
    public Mono<InterviewResponse> update(Long id, InterviewRequest request) {
        log.info("Updating interview: id={}", id);
        return interviewRepository.findById(id)
                .flatMap(existing -> interviewRepository.save(applyRequest(request, existing)))
                .map(this::toResponse)
                .doOnSuccess(saved -> log.info("Saved successfully: interview id={}", saved.getId()))
                .doOnError(error -> log.error("Failed to update interview id={}", id, error));
    }

    @Override
    public Mono<Void> delete(Long id) {
        log.info("Deleting interview: id={}", id);
        return interviewRepository.deleteById(id)
                .doOnSuccess(ignored -> log.info("Deleted interview: id={}", id))
                .doOnError(error -> log.error("Failed to delete interview id={}", id, error));
    }

    private Interview applyRequest(InterviewRequest request, Interview target) {
        target.setApplicationId(request.getApplicationId());
        target.setCandidateId(request.getCandidateId());
        target.setCandidateName(request.getCandidateName());
        target.setCandidateAvatar(request.getCandidateAvatar());
        target.setPosition(request.getPosition());
        target.setDate(request.getDate());
        target.setTime(request.getTime());
        target.setType(request.getType());
        target.setStatus(request.getStatus());
        target.setMeetingLink(request.getMeetingLink());
        target.setLocation(request.getLocation());
        target.setNotes(request.getNotes());
        return target;
    }

    private InterviewResponse toResponse(Interview interview) {
        return InterviewResponse.builder()
                .id(interview.getId())
                .applicationId(interview.getApplicationId())
                .candidateId(interview.getCandidateId())
                .candidateName(interview.getCandidateName())
                .candidateAvatar(interview.getCandidateAvatar())
                .position(interview.getPosition())
                .date(interview.getDate())
                .time(interview.getTime())
                .type(interview.getType())
                .status(interview.getStatus())
                .meetingLink(interview.getMeetingLink())
                .location(interview.getLocation())
                .notes(interview.getNotes())
                .build();
    }
}
