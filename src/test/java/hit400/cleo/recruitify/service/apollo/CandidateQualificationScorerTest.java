package hit400.cleo.recruitify.service.apollo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateQualificationScorerTest {

    @Test
    void scoresHigherWhenRequiredSkillsMatch() {
        CandidateQualificationScorer scorer = new CandidateQualificationScorer();

        CandidateQualificationScorer.ScoreResult result = scorer.score(
                "Jane Doe Senior Java Developer Spring Boot AWS Remote",
                List.of("Java", "Spring Boot"),
                List.of("Java", "Spring Boot"),
                List.of("AWS"),
                List.of("Developer"),
                List.of("Remote")
        );

        assertTrue(result.score() > 0.8, "Expected a high match score when required skills match");
        assertTrue(result.matchedRequired().contains("Java"));
        assertTrue(result.matchedRequired().contains("Spring Boot"));
    }
}

