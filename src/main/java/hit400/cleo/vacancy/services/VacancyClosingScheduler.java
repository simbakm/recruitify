package hit400.cleo.vacancy.services;

import hit400.cleo.vacancy.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VacancyClosingScheduler {

    private static final Logger log = LogManager.getLogger(VacancyClosingScheduler.class);

    private final VacancyRepository vacancyRepository;
    private final VacancyService vacancyService;

    @Scheduled(fixedDelayString = "${app.vacancy.closing.check-delay-ms:60000}")
    public void closeExpiredVacancies() {
        log.info("Running vacancy closing scheduler");
        vacancyRepository.findDueToClose("CLOSED", LocalDateTime.now())
                .flatMap(vacancy -> vacancyService.closeAndScore(vacancy.getId(), false))
                .subscribe(
                        ignored -> { },
                        error -> log.error("Vacancy closing scheduler failed", error)
                );
    }
}
