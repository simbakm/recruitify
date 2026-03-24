package hit400.cleo.recruitify.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HealthController {

    @GetMapping("/healthz")
    public Mono<ResponseEntity<String>> health() {
        return Mono.just(ResponseEntity.ok("ok"));
    }
}
