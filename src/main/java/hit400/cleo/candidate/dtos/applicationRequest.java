package hit400.cleo.candidate.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationRequest {

    private Long vacancyId;

    private Long candidateId;

    private String candidateName;

    private String candidateAvatar;

    private LocalDateTime appliedDate;

    private String status;

    private String resumeUrl;

    private String coverLetter;

    private String position;
}

