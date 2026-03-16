package hit400.cleo.recruitify;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.sql.init.mode=never"
})
class RecruitifyApplicationTests {

    @Test
    void contextLoads() {
    }

}
