package hit400.cleo.candidate.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewRequest {

    private Integer applicationId;
    private Integer candidateId;
    private String candidateName;
    private String candidateAvatar;
    private String position;
    private String date;
    private String time;
    private String type;
    private String status;
    private String meetingLink;
    private String location;
    private String notes;
}

