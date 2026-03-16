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
public class RecruiterResponse {

    private Long id;
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
        private boolean newApplications;
        private boolean interviewReminders;
        private boolean weeklyDigest;
        private boolean marketingEmails;
    }
}

