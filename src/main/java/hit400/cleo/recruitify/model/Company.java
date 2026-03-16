package hit400.cleo.recruitify.model;

import hit400.cleo.recruitify.model.enums.CompanySize;
import hit400.cleo.recruitify.model.enums.Industry;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("companies")
public class Company {

    @Id
    private Integer id;

    @Column("name")
    private String name;

    @Column("logo_url")
    private String logoUrl;

    @Column("industry")
    private Industry industry;

    @Column("size")
    private CompanySize size;

    @Column("website")
    private String website;

    @Column("location")
    private String location;

    @Column("description")
    private String description;

    @Column("culture")
    private String culture;

    @Column("social_links")
    private String socialLinks;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
