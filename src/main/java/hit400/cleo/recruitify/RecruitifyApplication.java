package hit400.cleo.recruitify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication(scanBasePackages = "hit400.cleo")
@EnableR2dbcRepositories(basePackages = "hit400.cleo")
public class RecruitifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecruitifyApplication.class, args);
    }

}
