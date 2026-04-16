package hit400.cleo.recruitify.controller;

import hit400.cleo.recruitify.dto.SerpApiCandidateSearchRequest;
import hit400.cleo.recruitify.dto.SerpApiCandidateSearchResponse;
import hit400.cleo.recruitify.service.serpapi.SerpApiCandidateSearchService;
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
@RequestMapping("api/serpapi/candidates")
@RequiredArgsConstructor
public class SerpApiCandidatesController {

    private final SerpApiCandidateSearchService serpApiCandidateSearchService;

    @PostMapping("/search")
    public Mono<ResponseEntity<SerpApiCandidateSearchResponse>> search(@RequestBody SerpApiCandidateSearchRequest request) {
        return serpApiCandidateSearchService.searchQualifiedCandidates(request)
                .map(candidates -> ResponseEntity.ok(new SerpApiCandidateSearchResponse(candidates)));
    }
}

