package hit400.cleo.candidate.services;

import hit400.cleo.candidate.dtos.InterviewRequest;
import hit400.cleo.candidate.dtos.InterviewResponse;
import hit400.cleo.candidate.models.Interview;
import hit400.cleo.candidate.repositories.CandidateInterviewRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CandidateInterviewServiceImpl implements CandidateInterviewService {

    private static final Logger log = LogManager.getLogger(CandidateInterviewServiceImpl.class);

    private final CandidateInterviewRepository interviewRepository;

    @Override
    public Mono<InterviewResponse> create(InterviewRequest request) {
        log.info("Creating candidate interview");
        Interview interview = applyRequest(request, new Interview());
        return interviewRepository.save(interview)
                .map(this::toResponse)
                .doOnSuccess(saved -> log.info("Saved successfully: candidate-interview id={}", saved.getId()))
                .doOnError(error -> log.error("Failed to create candidate interview", error));
    }

    @Override
    public Mono<InterviewResponse> getById(Long id) {
        log.info("Fetching candidate interview: id={}", id);
        return interviewRepository.findById(id).map(this::toResponse);
    }

    @Override
    public Flux<InterviewResponse> getAll() {
        log.info("Fetching all candidate interviews");
        return interviewRepository.findAll().map(this::toResponse);
    }

    @Override
    public Mono<InterviewResponse> update(Long id, InterviewRequest request) {
        log.info("Updating candidate interview: id={}", id);
        return interviewRepository.findById(id)
                .flatMap(existing -> interviewRepository.save(applyRequest(request, existing)))
                .map(this::toResponse)
                .doOnSuccess(saved -> log.info("Saved successfully: candidate-interview id={}", saved.getId()))
                .doOnError(error -> log.error("Failed to update candidate interview id={}", id, error));
    }

    @Override
    public Mono<Void> delete(Long id) {
        log.info("Deleting candidate interview: id={}", id);
        return interviewRepository.deleteById(id)
                .doOnSuccess(ignored -> log.info("Deleted candidate interview: id={}", id))
                .doOnError(error -> log.error("Failed to delete candidate interview id={}", id, error));
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
