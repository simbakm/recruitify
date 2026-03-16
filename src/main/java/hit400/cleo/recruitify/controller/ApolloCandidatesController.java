package hit400.cleo.recruitify.controller;

import hit400.cleo.recruitify.dto.ApolloCandidateSearchRequest;
import hit400.cleo.recruitify.dto.ApolloCandidateSearchResponse;
import hit400.cleo.recruitify.service.apollo.ApolloCandidateSearchService;
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
@RequestMapping("api/apollo/candidates")
@RequiredArgsConstructor
public class ApolloCandidatesController {

    private final ApolloCandidateSearchService apolloCandidateSearchService;

    @PostMapping("/search")
    public Mono<ResponseEntity<ApolloCandidateSearchResponse>> search(@RequestBody ApolloCandidateSearchRequest request) {
        return apolloCandidateSearchService.searchQualifiedCandidates(request)
                .map(candidates -> ResponseEntity.ok(new ApolloCandidateSearchResponse(candidates)));
    }
}

