package hit400.cleo.candidate.services;

import hit400.cleo.candidate.dtos.ApplicationRequest;
import hit400.cleo.candidate.dtos.ApplicationResponse;
import hit400.cleo.candidate.models.Application;
import hit400.cleo.candidate.repositories.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;

    @Override
    public Mono<ApplicationResponse> create(ApplicationRequest request) {
        Application application = applyRequest(request, new Application());
        if (application.getAppliedDate() == null) application.setAppliedDate(LocalDateTime.now());
        if (application.getStatus() == null || application.getStatus().isBlank()) application.setStatus("New");
        return applicationRepository.save(application).map(this::toResponse);
    }

    @Override
    public Mono<ApplicationResponse> getById(Long id) {
        return applicationRepository.findById(id).map(this::toResponse);
    }

    @Override
    public Flux<ApplicationResponse> getAll(Long vacancyId, Long candidateId, String status) {
        return applicationRepository.findFiltered(vacancyId, candidateId, status).map(this::toResponse);
    }

    @Override
    public Mono<ApplicationResponse> update(Long id, ApplicationRequest request) {
        return applicationRepository.findById(id)
                .flatMap(existing -> applicationRepository.save(applyRequest(request, existing)))
                .map(this::toResponse);
    }

    @Override
    public Mono<Void> delete(Long id) {
        return applicationRepository.deleteById(id);
    }

    private Application applyRequest(ApplicationRequest request, Application target) {
        target.setVacancyId(request.getVacancyId());
        target.setCandidateId(request.getCandidateId());
        target.setCandidateName(request.getCandidateName());
        target.setCandidateAvatar(request.getCandidateAvatar());
        target.setAppliedDate(request.getAppliedDate());
        target.setStatus(request.getStatus());
        target.setResumeUrl(request.getResumeUrl());
        target.setCoverLetter(request.getCoverLetter());
        target.setPosition(request.getPosition());
        return target;
    }

    private ApplicationResponse toResponse(Application application) {
        return ApplicationResponse.builder()
                .id(application.getId())
                .vacancyId(application.getVacancyId())
                .candidateId(application.getCandidateId())
                .candidateName(application.getCandidateName())
                .candidateAvatar(application.getCandidateAvatar())
                .appliedDate(application.getAppliedDate())
                .status(application.getStatus())
                .resumeUrl(application.getResumeUrl())
                .coverLetter(application.getCoverLetter())
                .position(application.getPosition())
                .build();
    }
}
