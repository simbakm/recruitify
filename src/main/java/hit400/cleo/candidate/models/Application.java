package hit400.cleo.candidate.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("applications")
public class Application {

    @Id
    private Long id;

    @Column("vacancy_id")
    private Long vacancyId;

    @Column("candidate_id")
    private Long candidateId;

    @Column("candidate_name")
    private String candidateName;

    @Column("candidate_avatar")
    private String candidateAvatar;

    @Column("applied_date")
    private LocalDateTime appliedDate;

    @Column("status")
    private String status;

    @Column("resume_url")
    private String resumeUrl;

    @Column("cover_letter")
    private String coverLetter;

    @Column("position")
    private String position;
}
