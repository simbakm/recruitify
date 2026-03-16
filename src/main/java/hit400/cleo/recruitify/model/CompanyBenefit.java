package hit400.cleo.recruitify.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("company_benefits")
public class CompanyBenefit {

    @Id
    private Integer id;

    @Column("company_id")
    private Integer companyId;

    @Column("benefit")
    private String benefit;

    @Column("created_at")
    private LocalDateTime createdAt;
}
