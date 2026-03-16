package hit400.cleo.recruiter.model;

import com.fasterxml.jackson.core.JsonProcessingException;
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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("recruiters")
public class Recruiter {

    @Id
    private Long id;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("email")
    private String email;

    @Column("phone")
    private String phone;

    @Column("role")
    private String role;

    @Column("avatar_url")
    private String avatarUrl;

    @Column("company_id")
    private Integer companyId;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("notification_preferences")
    private String notificationPreferencesJson;

    @Transient
    private NotificationPreferences notificationPreferences;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void setNotificationPreferences(NotificationPreferences notificationPreferences) {
        this.notificationPreferences = notificationPreferences;
        try {
            this.notificationPreferencesJson = notificationPreferences == null
                    ? null
                    : objectMapper.writeValueAsString(notificationPreferences);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert notification preferences to JSON", e);
        }
    }

    public NotificationPreferences getNotificationPreferences() {
        if (notificationPreferences == null && notificationPreferencesJson != null && !notificationPreferencesJson.isBlank()) {
            try {
                notificationPreferences = objectMapper.readValue(notificationPreferencesJson, NotificationPreferences.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse notification preferences from JSON", e);
            }
        }
        return notificationPreferences;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationPreferences {
        private boolean newApplications;
        private boolean interviewReminders;
        private boolean weeklyDigest;
        private boolean marketingEmails;
    }
}

