package hit400.cleo.recruitify.controller;

import hit400.cleo.recruitify.dto.CandidateProfileRequestDTO;
import hit400.cleo.recruitify.model.CandidateProfile;
import hit400.cleo.recruitify.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Step 1: account creation (minimal record).
     */
    @PostMapping("/account")
    public Mono<ResponseEntity<CandidateProfile>> createAccount(@RequestBody CandidateProfileRequestDTO dto) {
        return profileService.createAccount(dto)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved));
    }

   

    @GetMapping("/{id}")
    public Mono<ResponseEntity<CandidateProfile>> getProfile(@PathVariable Long id) {
        return profileService.getProfile(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Flux<CandidateProfile> getAllProfiles(@RequestParam(required = false) String email) {
        if (email != null && !email.isBlank()) {
            return profileService.findByEmail(email).flux();
        }
        return profileService.getAllProfiles();
    }

    @GetMapping("/search")
    public Mono<ResponseEntity<CandidateProfile>> findByEmail(@RequestParam String email) {
        if (email == null || email.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return profileService.findByEmail(email)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<CandidateProfile>> updateProfile(
            @PathVariable Long id,
            @RequestBody CandidateProfileRequestDTO dto) {
        return profileService.updateProfile(id, dto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteProfile(@PathVariable Long id) {
        return profileService.deleteProfile(id)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
