package hit400.cleo.recruitify.service;


import hit400.cleo.recruitify.dto.CandidateProfileRequestDTO;
import hit400.cleo.recruitify.model.CandidateProfile;
import hit400.cleo.recruitify.repository.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final CandidateProfileRepository repository;

    public Mono<CandidateProfile> getProfile(Long id) {
        return repository.findById(id);
    }

    public Flux<CandidateProfile> getAllProfiles() {
        return repository.findAll();
    }

    public Mono<CandidateProfile> createProfile(CandidateProfileRequestDTO dto) {
        if (dto.email() == null || dto.email().isBlank()) {
            return Mono.error(new IllegalArgumentException("email is required"));
        }

        return repository.findByEmail(dto.email())
                .flatMap(existing -> {
                    existing.setNew(false);
                    applyUpdates(existing, dto);
                    if (existing.getCreatedAt() == null) existing.setCreatedAt(LocalDateTime.now());
                    return repository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    if (dto.name() == null || dto.name().isBlank()) {
                        return Mono.error(new IllegalArgumentException("name is required"));
                    }
                    CandidateProfile profile = new CandidateProfile();
                    profile.setNew(true);
                    profile.setEmail(dto.email());
                    profile.setCreatedAt(LocalDateTime.now());
                    applyUpdates(profile, dto);
                    return repository.save(profile);
                }))
                .doOnSuccess(saved -> log.info("Saved successfully: profile id={}", saved.getId()));
    }

    public Mono<CandidateProfile> updateProfile(Long id, CandidateProfileRequestDTO dto) {
        return repository.findById(id)
                .flatMap(profile -> {
                    // Mark as not new so that save() performs an UPDATE
                    profile.setNew(false);   // if the field is named 'new'

                    if (dto.email() != null && !dto.email().isBlank() && !dto.email().equalsIgnoreCase(profile.getEmail())) {
                        return Mono.error(new IllegalArgumentException("Email cannot be changed"));
                    }

                    // Apply updates from DTO
                    applyUpdates(profile, dto);

                    return repository.save(profile);
                })
                .doOnSuccess(saved -> log.info("Saved successfully: profile id={}", saved.getId()));
    }

    public Mono<Void> deleteProfile(Long id) {
        return repository.deleteById(id);
    }

    private void applyUpdates(CandidateProfile profile, CandidateProfileRequestDTO dto) {
        if (dto.name() != null) profile.setName(dto.name());
        if (dto.phone() != null) profile.setPhone(dto.phone());
        if (dto.address() != null) profile.setAddress(dto.address());
        if (dto.objectives() != null) profile.setObjectives(dto.objectives());
        if (dto.lookingForJob() != null) profile.setLookingForJob(dto.lookingForJob());
        if (dto.desiredJobTitle() != null) profile.setDesiredJobTitle(dto.desiredJobTitle());
        if (dto.desiredCategory() != null) profile.setDesiredCategory(dto.desiredCategory());
        if (dto.preferredWorkMode() != null) profile.setPreferredWorkMode(dto.preferredWorkMode());
        if (dto.preferredLocation() != null) profile.setPreferredLocation(dto.preferredLocation());
        if (dto.salaryMin() != null) profile.setSalaryMin(dto.salaryMin());
        if (dto.salaryMax() != null) profile.setSalaryMax(dto.salaryMax());
        if (dto.salaryCurrency() != null) profile.setSalaryCurrency(dto.salaryCurrency());
        if (dto.skills() != null) profile.setSkills(dto.skills());
        if (dto.experiences() != null) profile.setExperiences(dto.experiences());
        if (dto.educations() != null) profile.setEducations(dto.educations());
    }
}
