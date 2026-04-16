package hit400.cleo.recruitify.dto;

public record SendEmailRequestDTO(
        String email,
        String subject,
        String body
) {}

