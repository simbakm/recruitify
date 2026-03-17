package hit400.cleo.recruitify.service;


import hit400.cleo.recruitify.dto.CandidateProfileRequestDTO;
import hit400.cleo.recruitify.model.CandidateProfile;
import hit400.cleo.recruitify.repository.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CandidateProfileRepository repository;

    public Mono<CandidateProfile> getProfile(Long id) {
        return repository.findById(id);
    }

    public Flux<CandidateProfile> getAllProfiles() {
        return repository.findAll();
    }

    public Mono<CandidateProfile> updateProfile(Long id, CandidateProfileRequestDTO dto) {
        return repository.findById(id)
                .flatMap(profile -> {
                    // Mark as not new so that save() performs an UPDATE
                    profile.setNew(false);   // if the field is named 'new'

                    // Apply updates from DTO
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

                    return repository.save(profile);
                });
    }

    public Mono<Void> deleteProfile(Long id) {
        return repository.deleteById(id);
    }
}
