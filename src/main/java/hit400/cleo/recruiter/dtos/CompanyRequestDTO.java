package hit400.cleo.recruiter.dtos;

import hit400.cleo.recruiter.model.Enums.CompanySize;
import hit400.cleo.recruitify.model.enums.Industry;
import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyRequestDTO {

    private String name;

    private String logoUrl;

    private Industry industry;

    private CompanySize size;

    private String website;

    private String location;

    private String description;

    private String culture;

    private List<String> benefits;

    private Map<String, String> socialLinks;
}
