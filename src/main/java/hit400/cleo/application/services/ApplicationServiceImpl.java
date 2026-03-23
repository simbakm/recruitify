package hit400.cleo.application.services;

import hit400.cleo.application.dtos.ApplicationRequest;
import hit400.cleo.application.dtos.ApplicationResponse;
import hit400.cleo.application.dtos.ApplicationStatusUpdateRequest;
import hit400.cleo.application.model.Application;
import hit400.cleo.application.model.enums.ApplicationStatus;
import hit400.cleo.application.repository.JobApplicationRepository;
import hit400.cleo.recruitify.model.CandidateProfile;
import hit400.cleo.recruitify.repository.CandidateProfileRepository;
import hit400.cleo.vacancy.model.Vacancy;
import hit400.cleo.vacancy.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service("jobApplicationServiceImpl")
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger log = LogManager.getLogger(ApplicationServiceImpl.class);

    private final JobApplicationRepository applicationRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final VacancyRepository vacancyRepository;

    @Override
    public Mono<ApplicationResponse> create(Long vacancyId, Long candidateId, ApplicationRequest request) {
        log.info("Creating application: vacancyId={}, candidateId={}", vacancyId, candidateId);
        if (vacancyId == null || candidateId == null) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "vacancyId and candidateId are required"));
        }

        String coverLetter = request != null ? normalizeCoverLetter(request.getCoverLetter()) : null;

        Mono<CandidateProfile> candidateMono = candidateProfileRepository.findById(candidateId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Candidate profile not found")));

        Mono<Vacancy> vacancyMono = vacancyRepository.findById(vacancyId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Vacancy not found")));

        return Mono.zip(candidateMono, vacancyMono)
                .flatMap(tuple -> {
                    CandidateProfile candidate = tuple.getT1();
                    Vacancy vacancy = tuple.getT2();

                    if (candidate.getName() == null || candidate.getName().isBlank()) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Candidate name is required"));
                    }
                    if (vacancy.getTitle() == null || vacancy.getTitle().isBlank()) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Vacancy title is required"));
                    }

                    Application application = Application.builder()
                            .vacancyId(vacancy.getId())
                            .candidateId(candidate.getId())
                            .candidateName(candidate.getName())
                            .candidateAvatar(candidate.getProfilePic())
                            .appliedDate(LocalDateTime.now())
                            .status(ApplicationStatus.New)
                            .resumeUrl(candidate.getCvFilePath())
                            .coverLetter(coverLetter)
                            .position(vacancy.getTitle())
                            .build();

                    return applicationRepository.save(application).map(this::toResponse);
                })
                .doOnSuccess(saved -> log.info("Application created: id={} vacancyId={} candidateId={}",
                        saved.getId(), saved.getVacancyId(), saved.getCandidateId()))
                .doOnError(error -> log.error("Failed to create application: vacancyId={} candidateId={}",
                        vacancyId, candidateId, error));
    }

    @Override
    public Flux<ApplicationResponse> getByCandidateId(Long candidateId) {
        log.info("Fetching applications by candidateId={}", candidateId);
        return applicationRepository.findByCandidateId(candidateId).map(this::toResponse);
    }

    @Override
    public Flux<ApplicationResponse> getByVacancyId(Long vacancyId) {
        log.info("Fetching applications by vacancyId={}", vacancyId);
        return applicationRepository.findByVacancyId(vacancyId).map(this::toResponse);
    }

    @Override
    public Mono<ApplicationResponse> updateStatus(Long applicationId, ApplicationStatusUpdateRequest request) {
        log.info("Updating application status: applicationId={}", applicationId);
        if (request == null || request.getStatus() == null) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid status. Allowed: " + allowedStatuses()));
        }

        return applicationRepository.findById(applicationId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application not found")))
                .flatMap(existing -> {
                    existing.setStatus(request.getStatus());
                    return applicationRepository.save(existing);
                })
                .map(this::toResponse)
                .doOnSuccess(saved -> log.info("Application status updated: id={} status={}",
                        saved.getId(), saved.getStatus()))
                .doOnError(error -> log.error("Failed to update application status: id={}", applicationId, error));
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
                .score(application.getScore())
                .threshold(application.getThreshold())
                .resumeUrl(application.getResumeUrl())
                .coverLetter(application.getCoverLetter())
                .position(application.getPosition())
                .build();
    }

    private String normalizeCoverLetter(String coverLetter) {
        if (coverLetter == null) return null;
        return coverLetter.replace("\r\n", "\n").replace("\r", "\n");
    }

    private String allowedStatuses() {
        ApplicationStatus[] values = ApplicationStatus.values();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) builder.append(", ");
            builder.append(values[i].name());
        }
        return builder.toString();
    }
}
