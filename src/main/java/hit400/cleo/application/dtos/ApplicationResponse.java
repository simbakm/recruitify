package hit400.cleo.application.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import hit400.cleo.application.model.enums.ApplicationStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {

    private Long id;
    private Long vacancyId;
    private Long candidateId;
    private String candidateName;
    private String candidateAvatar;
    private LocalDateTime appliedDate;
    private ApplicationStatus status;
    private Double score;
    private Double threshold;
    private String resumeUrl;
    private String coverLetter;
    private String position;
}
