package hit400.cleo.vacancy.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("vacancies")
public class Vacancy {

    @Id
    private Long id;

    @Column("title")
    private String title;

    @Column("category")
    private String category;

    @Column("location")
    private String location;

    @Column("employment_type")
    private String employmentType;

    @Column("salary_min")
    private Integer salaryMin;

    @Column("salary_max")
    private Integer salaryMax;

    @Column("salary_currency")
    private String salaryCurrency;

    @Column("description")
    private String description;

    @Column("requirements")
    private String requirementsJson;

    @Column("status")
    private String status;

    @Column("posted_date")
    private LocalDateTime postedDate;

    @Column("company_id")
    private Integer companyId;

    @Column("applicant_count")
    private Integer applicantCount;

    @Transient
    @Builder.Default
    private List<String> requirements = new ArrayList<>();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void setRequirements(List<String> requirements) {
        this.requirements = requirements != null ? requirements : new ArrayList<>();
        try {
            this.requirementsJson = objectMapper.writeValueAsString(this.requirements);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert requirements to JSON", e);
        }
    }

    public List<String> getRequirements() {
        if (requirements.isEmpty() && requirementsJson != null && !requirementsJson.isBlank()) {
            try {
                requirements = objectMapper.readValue(requirementsJson, new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse requirements from JSON", e);
            }
        }
        return requirements;
    }
}
