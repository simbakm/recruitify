package hit400.cleo.recruitify.controller;

import hit400.cleo.recruitify.dto.SendEmailRequestDTO;
import hit400.cleo.recruitify.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/send")
    public Mono<ResponseEntity<Void>> sendEmail(@RequestBody SendEmailRequestDTO request) {
        if (request == null
                || request.email() == null || request.email().isBlank()
                || request.subject() == null || request.subject().isBlank()
                || request.body() == null || request.body().isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return emailService.sendEmail(request.email(), request.subject(), request.body())
                .thenReturn(ResponseEntity.ok().build());
    }
}

