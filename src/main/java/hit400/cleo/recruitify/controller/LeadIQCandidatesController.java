package hit400.cleo.recruitify.controller;
 
import hit400.cleo.recruitify.dto.LeadIQCandidateSearchRequest;
import hit400.cleo.recruitify.dto.LeadIQCandidateSearchResponse;
import hit400.cleo.recruitify.service.leadiq.LeadIQCandidateSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
 
@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("api/leadiq/candidates")
@RequiredArgsConstructor
public class LeadIQCandidatesController {
 
    private final LeadIQCandidateSearchService leadiqCandidateSearchService;
 
    @PostMapping("/search")
    public Mono<ResponseEntity<LeadIQCandidateSearchResponse>> search(@RequestBody LeadIQCandidateSearchRequest request) {
        return leadiqCandidateSearchService.searchQualifiedCandidates(request)
                .map(candidates -> ResponseEntity.ok(new LeadIQCandidateSearchResponse(candidates)));
    }
}
