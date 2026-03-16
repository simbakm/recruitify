package hit400.cleo.recruitify.controller;

import hit400.cleo.recruitify.dto.CandidateProfileRequestDTO;
import hit400.cleo.recruitify.model.CandidateProfile;
import hit400.cleo.recruitify.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/{id}")
    public Mono<CandidateProfile> getProfile(@PathVariable Long id) {
        return profileService.getProfile(id);
    }

    @GetMapping
    public Flux<CandidateProfile> getAllProfiles() {
        return profileService.getAllProfiles();
    }

    @PutMapping("/{id}")
    public Mono<CandidateProfile> updateProfile(
            @PathVariable Long id,
            @RequestBody CandidateProfileRequestDTO dto) {

        return profileService.updateProfile(id, dto);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteProfile(@PathVariable Long id) {
        return profileService.deleteProfile(id);
    }
}