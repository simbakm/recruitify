package hit400.cleo.recruitify.controller;

import hit400.cleo.recruitify.dto.SnovIoEmailFinderRequest;
import hit400.cleo.recruitify.dto.SnovIoEmailFinderResponse;
import hit400.cleo.recruitify.service.snovio.SnovIoEmailFinderService;
import hit400.cleo.recruitify.service.snovio.SnovIoProperties;
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
@RequestMapping("api/snovio/email-finder")
@RequiredArgsConstructor
public class SnovIoEmailFinderController {

    private final SnovIoEmailFinderService snovIoEmailFinderService;
    private final SnovIoProperties properties;

    /**
     * Finds an email for a candidate by name.
     * Note: Snov.io "name" lookup requires a domain; if not provided in request,
     * this endpoint uses `snovio.default-domain`.
     */
    @PostMapping("/search")
    public Mono<ResponseEntity<SnovIoEmailFinderResponse>> search(@RequestBody SnovIoEmailFinderRequest request) {
        if (request == null || request.fullName() == null || request.fullName().isBlank()) {
            return Mono.error(new IllegalArgumentException("fullName is required"));
        }

        String domain = request.domain();
        if (domain == null || domain.isBlank()) {
            domain = properties.defaultDomain();
        }
        if (domain == null || domain.isBlank()) {
            return Mono.error(new IllegalArgumentException("domain is required (or configure snovio.default-domain)"));
        }

        String finalDomain = domain;
        return snovIoEmailFinderService.findEmailByFullName(request.fullName(), finalDomain)
                .map(r -> ResponseEntity.ok(new SnovIoEmailFinderResponse(r.fullName(), r.domain(), r.email())));
    }
}

