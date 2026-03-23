package hit400.cleo.recruitify.service;


import hit400.cleo.recruitify.dto.CandidateProfileRequestDTO;
import hit400.cleo.recruitify.exception.ConflictException;
import hit400.cleo.recruitify.exception.NotFoundException;
import hit400.cleo.recruitify.model.CandidateProfile;
import hit400.cleo.recruitify.repository.CandidateProfileRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final Logger log = LogManager.getLogger(ProfileService.class);

    private static final String PROFILE_PIC_DIR = "uploads/profile/";
    private final CandidateProfileRepository repository;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(PROFILE_PIC_DIR));
            log.info("Profile picture directory created/verified: {}", PROFILE_PIC_DIR);
        } catch (IOException e) {
            log.error("Failed to create profile picture directory", e);
            throw new RuntimeException("Could not initialize profile picture directory", e);
        }
    }

    public Mono<CandidateProfile> getProfile(Long id) {
        log.info("Fetching profile: id={}", id);
        return repository.findById(id);
    }

    public Flux<CandidateProfile> getAllProfiles() {
        log.info("Fetching all profiles");
        return repository.findAll();
    }

    public Mono<CandidateProfile> findByEmail(String email) {
        if (email == null) return Mono.empty();
        final String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return Mono.empty();
        return repository.findByEmailIgnoreCase(normalized)
                .doOnNext((profile) -> profile.setNew(false));
    }


    public Mono<CandidateProfile> createAccount(CandidateProfileRequestDTO dto) {
        if (dto.email() == null || dto.email().isBlank()) {
            return Mono.error(new IllegalArgumentException("email is required"));
        }
        if (dto.name() == null || dto.name().isBlank()) {
            return Mono.error(new IllegalArgumentException("name is required"));
        }

        final String normalizedEmail = dto.email().trim().toLowerCase(Locale.ROOT);

        return repository.findByEmailIgnoreCase(normalizedEmail)
                .flatMap(existing -> Mono.<CandidateProfile>error(new ConflictException("Account already exists for email: " + normalizedEmail)))
                .switchIfEmpty(Mono.defer(() -> {
                    CandidateProfile profile = new CandidateProfile();
                    profile.setNew(true);
                    profile.setName(dto.name());
                    profile.setEmail(normalizedEmail);
                    if (dto.phone() != null && !dto.phone().isBlank()) {
                        profile.setPhone(dto.phone());
                    }
                    if (dto.address() != null && !dto.address().isBlank()) {
                        profile.setAddress(dto.address());
                    }
                    profile.setCreatedAt(LocalDateTime.now());
                    return repository.save(profile);
                }))
                .doOnSuccess(saved -> log.info("Created account: profile id={} email={}", saved.getId(), saved.getEmail()));
    }

    public Mono<CandidateProfile> updateProfile(Long id, CandidateProfileRequestDTO dto) {
        log.info("Updating profile: id={}", id);
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
                .doOnSuccess(saved -> log.info("Saved successfully: profile id={}", saved.getId()))
                .doOnError(error -> log.error("Failed to update profile id={}", id, error));
    }

    public Mono<Void> deleteProfile(Long id) {
        log.info("Deleting profile: id={}", id);
        return repository.deleteById(id)
                .doOnSuccess(ignored -> log.info("Deleted profile: id={}", id))
                .doOnError(error -> log.error("Failed to delete profile id={}", id, error));
    }

    public Mono<CandidateProfile> updateProfileAvatar(Long id, FilePart filePart) {
        log.info("Uploading profile avatar: id={}", id);
        if (filePart == null) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "profilePic file is required"));
        }

        return repository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Candidate profile not found")))
                .flatMap(profile -> {
                    profile.setNew(false);
                    String filename = System.currentTimeMillis() + "_" + filePart.filename();
                    Path path = Paths.get(PROFILE_PIC_DIR + filename);
                    return filePart.transferTo(path)
                            .then(Mono.defer(() -> {
                                profile.setProfilePic(path.toString());
                                return repository.save(profile);
                            }));
                })
                .doOnSuccess(saved -> log.info("Updated profile avatar: id={}", saved.getId()))
                .doOnError(error -> log.error("Failed to update profile avatar: id={}", id, error));
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
