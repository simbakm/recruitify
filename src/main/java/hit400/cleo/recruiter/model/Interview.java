package hit400.cleo.recruiter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("interviews")
public class Interview {

    @Id
    private Long id;

    @Column("application_id")
    private Integer applicationId;

    @Column("candidate_id")
    private Integer candidateId;

    @Column("candidate_name")
    private String candidateName;

    @Column("candidate_avatar")
    private String candidateAvatar;

    @Column("position")
    private String position;

    @Column("interview_date")
    private String date;

    @Column("interview_time")
    private String time;

    @Column("type")
    private String type;

    @Column("status")
    private String status;

    @Column("meeting_link")
    private String meetingLink;

    @Column("location")
    private String location;

    @Column("notes")
    private String notes;
}

