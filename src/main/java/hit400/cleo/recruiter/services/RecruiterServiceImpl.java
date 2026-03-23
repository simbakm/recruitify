package hit400.cleo.recruiter.services;

import hit400.cleo.recruiter.dtos.RecruiterRequest;
import hit400.cleo.recruiter.dtos.RecruiterResponse;
import hit400.cleo.recruiter.model.Recruiter;
import hit400.cleo.recruiter.repository.RecruiterRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RecruiterServiceImpl implements RecruiterService {

    private static final Logger log = LogManager.getLogger(RecruiterServiceImpl.class);

    private final RecruiterRepository recruiterRepository;

    @Override
    public Mono<RecruiterResponse> create(RecruiterRequest request) {
        log.info("Creating recruiter");
        Recruiter recruiter = applyRequest(request, new Recruiter(), true);
        if (recruiter.getCreatedAt() == null) recruiter.setCreatedAt(LocalDateTime.now());
        return recruiterRepository.save(recruiter)
                .map(this::toResponse)
                .doOnSuccess(saved -> log.info("Saved successfully: recruiter id={}", saved.getId()))
                .doOnError(error -> log.error("Failed to create recruiter", error));
    }

    @Override
    public Mono<RecruiterResponse> getById(Long id) {
        log.info("Fetching recruiter: id={}", id);
        return recruiterRepository.findById(id).map(this::toResponse);
    }

    @Override
    public Flux<RecruiterResponse> getAll() {
        log.info("Fetching all recruiters");
        return recruiterRepository.findAll().map(this::toResponse);
    }

    @Override
    public Mono<RecruiterResponse> update(Long id, RecruiterRequest request) {
        log.info("Updating recruiter: id={}", id);
        return recruiterRepository.findById(id)
                .flatMap(existing -> recruiterRepository.save(applyRequest(request, existing, false)))
                .map(this::toResponse)
                .doOnSuccess(saved -> log.info("Saved successfully: recruiter id={}", saved.getId()))
                .doOnError(error -> log.error("Failed to update recruiter id={}", id, error));
    }

    @Override
    public Mono<Void> delete(Long id) {
        log.info("Deleting recruiter: id={}", id);
        return recruiterRepository.deleteById(id)
                .doOnSuccess(ignored -> log.info("Deleted recruiter: id={}", id))
                .doOnError(error -> log.error("Failed to delete recruiter id={}", id, error));
    }

    private Recruiter applyRequest(RecruiterRequest request, Recruiter target, boolean isCreate) {
        target.setFirstName(request.getFirstName());
        target.setLastName(request.getLastName());
        target.setEmail(request.getEmail());
        target.setPhone(request.getPhone());
        target.setRole(request.getRole());
        target.setAvatarUrl(request.getAvatarUrl());
        target.setCompanyId(request.getCompanyId());

        if (isCreate) {
            target.setCreatedAt(request.getCreatedAt());
        }

        if (request.getNotificationPreferences() != null) {
            RecruiterRequest.NotificationPreferences prefs = request.getNotificationPreferences();
            target.setNotificationPreferences(Recruiter.NotificationPreferences.builder()
                    .newApplications(Boolean.TRUE.equals(prefs.getNewApplications()))
                    .interviewReminders(Boolean.TRUE.equals(prefs.getInterviewReminders()))
                    .weeklyDigest(Boolean.TRUE.equals(prefs.getWeeklyDigest()))
                    .marketingEmails(Boolean.TRUE.equals(prefs.getMarketingEmails()))
                    .build());
        } else if (isCreate) {
            target.setNotificationPreferences(null);
        }

        return target;
    }

    private RecruiterResponse toResponse(Recruiter recruiter) {
        Recruiter.NotificationPreferences prefs = recruiter.getNotificationPreferences();
        RecruiterResponse.NotificationPreferences responsePrefs = prefs == null
                ? null
                : RecruiterResponse.NotificationPreferences.builder()
                .newApplications(prefs.isNewApplications())
                .interviewReminders(prefs.isInterviewReminders())
                .weeklyDigest(prefs.isWeeklyDigest())
                .marketingEmails(prefs.isMarketingEmails())
                .build();

        return RecruiterResponse.builder()
                .id(recruiter.getId())
                .firstName(recruiter.getFirstName())
                .lastName(recruiter.getLastName())
                .email(recruiter.getEmail())
                .phone(recruiter.getPhone())
                .role(recruiter.getRole())
                .avatarUrl(recruiter.getAvatarUrl())
                .companyId(recruiter.getCompanyId())
                .createdAt(recruiter.getCreatedAt())
                .notificationPreferences(responsePrefs)
                .build();
    }
}
