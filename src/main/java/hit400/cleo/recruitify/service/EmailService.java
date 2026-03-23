package hit400.cleo.recruitify.service;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LogManager.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromAddress;

    public Mono<Void> sendEmail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            return Mono.empty();
        }

        return Mono.fromRunnable(() -> {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(fromAddress);
                    message.setTo(to);
                    message.setSubject(subject);
                    message.setText(body);
                    mailSender.send(message);
                })
                .doOnSuccess(ignored -> log.info("Email sent to={} subject={}", to, subject))
                .doOnError(error -> log.error("Failed to send email to={}", to, error))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
