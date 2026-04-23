package hit400.cleo.recruitify.service.serpapi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SerpApiCandidateSearchServiceTest {

    @Test
    void buildQueryIncludesPositionSkillsAndSite() {
        String q = SerpApiCandidateSearchService.buildQuery(
                "Software Engineer",
                List.of("Java", "Spring Boot"),
                List.of("AWS"),
                List.of(),
                "linkedin.com/in"
        );

        assertTrue(q.contains("\"Software Engineer\""));
        assertTrue(q.contains("site:linkedin.com/in"));
        assertTrue(q.contains("\"Java\""));
        assertTrue(q.contains("\"Spring Boot\""));
        assertTrue(q.contains("\"AWS\""));
    }

    @Test
    void clampLimitEnforcesMinimumOfFiveByDefault() {
        int limit = SerpApiCandidateSearchService.clampLimit(1, 10, 5);
        assertTrue(limit >= 5);
    }
}
