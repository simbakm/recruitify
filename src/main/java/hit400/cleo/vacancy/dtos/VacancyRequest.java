package hit400.cleo.vacancy.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VacancyRequest {

    private String title;
    private String category;
    private String location;
    private String employmentType;

    private SalaryRange salaryRange;

    private String description;
    private List<String> requirements;
    private String status;
    private LocalDateTime postedDate;
    private Integer companyId;
    private Integer applicantCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SalaryRange {
        private Integer min;
        private Integer max;
        private String currency;
    }
}

