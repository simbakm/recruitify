package hit400.cleo.recruitify.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("candidate_profiles")
public class CandidateProfile implements Persistable<Long> {

    @Id
    private Long id;

    @Column("name")
    private String name;

    @Column("email")
    private String email;

    @Column("phone")
    private String phone;

    @Column("address")
    private String address;

    @Column("objectives")
    private String objectives;

    // Store as JSON strings in database
    @Column("experiences")
    private String experiencesJson;

    @Column("educations")
    private String educationsJson;

    @Column("skills")
    private String skillsJson;

    @Column("cv_file_path")
    private String cvFilePath;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Transient
    private boolean isNew = true;

    @Transient
    private List<Experience> experiences = new ArrayList<>();

    @Transient
    private List<Education> educations = new ArrayList<>();

    @Transient
    private List<String> skills = new ArrayList<>();

    // ObjectMapper for JSON conversion
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public boolean isNew() {
        return isNew;
    }

    // Custom setters/getters for JSON conversion
    public void setExperiences(List<Experience> experiences) {
        this.experiences = experiences != null ? experiences : new ArrayList<>();
        try {
            this.experiencesJson = objectMapper.writeValueAsString(this.experiences);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert experiences to JSON", e);
        }
    }

    public List<Experience> getExperiences() {
        if (experiences.isEmpty() && experiencesJson != null && !experiencesJson.isEmpty()) {
            try {
                experiences = objectMapper.readValue(experiencesJson,
                        new TypeReference<List<Experience>>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse experiences from JSON", e);
            }
        }
        return experiences;
    }

    public void setEducations(List<Education> educations) {
        this.educations = educations != null ? educations : new ArrayList<>();
        try {
            this.educationsJson = objectMapper.writeValueAsString(this.educations);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert educations to JSON", e);
        }
    }

    public List<Education> getEducations() {
        if (educations.isEmpty() && educationsJson != null && !educationsJson.isEmpty()) {
            try {
                educations = objectMapper.readValue(educationsJson,
                        new TypeReference<List<Education>>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse educations from JSON", e);
            }
        }
        return educations;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills != null ? skills : new ArrayList<>();
        try {
            this.skillsJson = objectMapper.writeValueAsString(this.skills);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert skills to JSON", e);
        }
    }

    public List<String> getSkills() {
        if (skills.isEmpty() && skillsJson != null && !skillsJson.isEmpty()) {
            try {
                skills = objectMapper.readValue(skillsJson,
                        new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse skills from JSON", e);
            }
        }
        return skills;
    }

    // Nested classes
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Experience {
        private String jobTitle;
        private String company;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Education {
        private String degree;
        private String institution;
        private LocalDateTime graduationYear;
        private String description;
    }
}