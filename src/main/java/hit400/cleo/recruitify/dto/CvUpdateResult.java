package hit400.cleo.recruitify.dto;

import hit400.cleo.recruitify.model.CandidateProfile;

import java.util.List;

public record CvUpdateResult(
        CandidateProfile profile,
        List<String> warnings
) {}
