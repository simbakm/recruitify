package hit400.cleo.recruiter.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruiterRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String role;
    private String avatarUrl;
    private Integer companyId;
    private LocalDateTime createdAt;
    private NotificationPreferences notificationPreferences;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationPreferences {
        private Boolean newApplications;
        private Boolean interviewReminders;
        private Boolean weeklyDigest;
        private Boolean marketingEmails;
    }
}

