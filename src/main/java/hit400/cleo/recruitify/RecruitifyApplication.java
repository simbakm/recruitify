package hit400.cleo.recruitify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication(scanBasePackages = "hit400.cleo")
@EnableR2dbcRepositories(basePackages = "hit400.cleo")
@EnableScheduling
public class RecruitifyApplication {

    private static final Logger log = LogManager.getLogger(RecruitifyApplication.class);

    @PostConstruct
    public void ensureLogsDirectory() {
        try {
            Files.createDirectories(Paths.get("logs"));
            log.info("Logs directory created/verified");
        } catch (IOException e) {
            log.error("Failed to create logs directory", e);
        }
    }

    public static void main(String[] args) {
        try {
            Files.createDirectories(Paths.get("logs"));
        } catch (IOException e) {
            System.err.println("Failed to create logs directory: " + e.getMessage());
        }
        SpringApplication.run(RecruitifyApplication.class, args);
    }

}
