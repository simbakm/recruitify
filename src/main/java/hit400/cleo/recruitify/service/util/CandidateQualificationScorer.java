package hit400.cleo.recruitify.service.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CandidateQualificationScorer {

    public record ScoreResult(
            double score,
            List<String> matchedRequired,
            List<String> matchedOptional
    ) {}

    public ScoreResult score(
            String searchableText,
            List<String> candidateSkills,
            List<String> requiredSkills,
            List<String> optionalSkills,
            List<String> titleKeywords,
            List<String> locations
    ) {
        String haystack = normalize(searchableText);
        Set<String> skillSet = new LinkedHashSet<>();
        if (candidateSkills != null) {
            for (String s : candidateSkills) {
                if (s != null && !s.isBlank()) skillSet.add(normalize(s));
            }
        }

        List<String> matchedRequired = matchSkills(haystack, skillSet, requiredSkills);
        List<String> matchedOptional = matchSkills(haystack, skillSet, optionalSkills);

        double requiredScore = fraction(matchedRequired.size(), safeSize(requiredSkills));
        double optionalScore = fraction(matchedOptional.size(), safeSize(optionalSkills));
        double titleScore = fraction(matchKeywords(haystack, titleKeywords), safeSize(titleKeywords));
        double locationScore = fraction(matchKeywords(haystack, locations), safeSize(locations));

        double score = (0.55 * requiredScore) + (0.20 * optionalScore) + (0.15 * titleScore) + (0.10 * locationScore);
        return new ScoreResult(score, matchedRequired, matchedOptional);
    }

    private static int matchKeywords(String haystack, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return 0;
        int count = 0;
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) continue;
            if (haystack.contains(normalize(keyword))) count++;
        }
        return count;
    }

    private static List<String> matchSkills(String haystack, Set<String> candidateSkills, List<String> skills) {
        if (skills == null || skills.isEmpty()) return List.of();
        Set<String> matched = new LinkedHashSet<>();
        for (String skill : skills) {
            if (skill == null || skill.isBlank()) continue;
            String normalized = normalize(skill);
            if (candidateSkills.contains(normalized) || haystack.contains(normalized)) {
                matched.add(skill.trim());
            }
        }
        return new ArrayList<>(matched);
    }

    private static int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static double fraction(int numerator, int denominator) {
        if (denominator <= 0) return 0.0;
        return Math.max(0.0, Math.min(1.0, (double) numerator / (double) denominator));
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT).trim();
    }
}

