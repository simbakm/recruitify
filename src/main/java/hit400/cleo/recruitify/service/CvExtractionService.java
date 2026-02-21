package hit400.cleo.recruitify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hit400.cleo.recruitify.dto.CvUploadRequest;
import hit400.cleo.recruitify.model.CandidateProfile;
import hit400.cleo.recruitify.repository.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CvExtractionService {

    private final CandidateProfileRepository candidateProfileRepository;

    private final ObjectMapper objectMapper;


    // Python microservice URL
    private static final String PYTHON_MICROSERVICE_URL = "http://localhost:5000/parse";

    // DateTime formatters for various date formats
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy"),                    // 2016
            DateTimeFormatter.ofPattern("yyyy-MM"),                  // 2016-01
            DateTimeFormatter.ofPattern("yyyy/MM"),                  // 2016/01
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),               // 2016-01-15
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),               // 2016/01/15
            DateTimeFormatter.ofPattern("MMMM yyyy"),                // January 2016
            DateTimeFormatter.ofPattern("MMM yyyy"),                 // Jan 2016
            DateTimeFormatter.ofPattern("dd MMM yyyy"),              // 15 Jan 2016
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),               // 01/15/2016
            DateTimeFormatter.ofPattern("dd/MM/yyyy")                // 15/01/2016
    );

    public Mono<CandidateProfile> extractAndSaveProfile(CvUploadRequest request) {
        return callPythonMicroservice(request.cvFile())
                .flatMap(this::parseStructuredData)
                .flatMap(candidateProfileRepository::save)
                .doOnSuccess(profile -> log.info("Successfully saved profile for: {}", profile.getEmail()))
                .doOnError(error -> log.error("Failed to process CV: {}", error.getMessage()));
    }

    /**
     * Call the Python microservice API to parse the PDF
     */
    private Mono<JsonNode> callPythonMicroservice(FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .flatMap(fileBytes -> {
                    // Create WebClient instance
                    WebClient webClient = WebClient.builder()
                            .baseUrl(PYTHON_MICROSERVICE_URL)
                            .build();

                    // Build multipart request
                    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
                    bodyBuilder.part("file", fileBytes)
                            .filename(filePart.filename())
                            .contentType(MediaType.APPLICATION_PDF);

                    log.info("Sending PDF to Python microservice: {}", filePart.filename());

                    return webClient.post()
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                            .retrieve()
                            .bodyToMono(String.class)
                            .map(response -> {
                                log.debug("Python microservice response: {}", response);
                                try {
                                    return objectMapper.readTree(response);
                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to parse microservice response", e);
                                }
                            })
                            .doOnSuccess(json -> log.info("Successfully received parsed data from microservice"))
                            .doOnError(error -> log.error("Error calling Python microservice: {}", error.getMessage()));
                });
    }

    /**
     * Parse the JSON response from the microservice into a CandidateProfile
     */
    private Mono<CandidateProfile> parseStructuredData(JsonNode jsonNode) {
        return Mono.fromCallable(() -> {
            CandidateProfile profile = new CandidateProfile();

            // Extract the data object from the response
            JsonNode dataNode = jsonNode.has("data") ? jsonNode.get("data") : jsonNode;

            // Basic information with null handling
            String name = getJsonText(dataNode, "name");
            if (name == null || name.isEmpty()) {
                // Try to extract name from other fields or set a default
                name = getJsonText(dataNode, "candidate_name", "full_name", "applicant_name");

                // If still null, log warning and set a placeholder
                if (name == null || name.isEmpty()) {
                    log.warn("Name not found in parsed data. Available fields: {}", dataNode.fieldNames());
                    name = "Unknown Candidate";
                }
            }
            profile.setName(name);

            // Email with validation
            String email = getJsonText(dataNode, "email");
            if (email == null || email.isEmpty()) {
                log.warn("Email not found in parsed data");
                // You might want to generate a placeholder or skip
            }
            profile.setEmail(email);

            // Phone (optional)
            profile.setPhone(getJsonText(dataNode, "phone"));

            // Address (optional)
            profile.setAddress(getJsonText(dataNode, "location", "address"));

            // Objectives (optional)
            profile.setObjectives(getJsonText(dataNode, "objectives", "objective", "summary"));

            // Parse skills (handle null)
            List<String> skills = new ArrayList<>();
            JsonNode skillsNode = dataNode.get("skills");
            if (skillsNode != null && skillsNode.isArray()) {
                skillsNode.forEach(skill -> {
                    String skillText = skill.asText();
                    if (skillText != null && !skillText.isEmpty()) {
                        skills.add(skillText);
                    }
                });
            }
            profile.setSkills(skills);

            // Parse education (handle null)
            List<CandidateProfile.Education> educations = new ArrayList<>();
            JsonNode educationNode = dataNode.get("education");
            if (educationNode != null && educationNode.isArray()) {
                educationNode.forEach(edu -> {
                    CandidateProfile.Education education = new CandidateProfile.Education();

                    String degree = getJsonText(edu, "qualification", "degree", "course");
                    if (degree != null) education.setDegree(degree);

                    String institution = getJsonText(edu, "institution", "school", "university", "college");
                    if (institution != null) education.setInstitution(institution);

                    String yearStr = getJsonText(edu, "year", "graduationYear", "date");
                    if (yearStr != null) {
                        education.setGraduationYear(parseDate(yearStr));
                    }

                    // Only add if we have at least some data
                    if (degree != null || institution != null) {
                        educations.add(education);
                    }
                });
            }
            profile.setEducations(educations);

            // Parse experience (handle null)
            List<CandidateProfile.Experience> experiences = new ArrayList<>();
            JsonNode experienceNode = dataNode.get("experience");
            if (experienceNode != null && experienceNode.isArray()) {
                experienceNode.forEach(exp -> {
                    CandidateProfile.Experience experience = new CandidateProfile.Experience();

                    String jobTitle = getJsonText(exp, "job_title", "jobTitle", "title", "role");
                    if (jobTitle != null) experience.setJobTitle(jobTitle);

                    String company = getJsonText(exp, "company", "employer", "organization");
                    if (company != null) experience.setCompany(company);

                    String description = getJsonText(exp, "description", "details");
                    if (description != null) experience.setDescription(description);

                    String startDateStr = getJsonText(exp, "start_date", "startDate", "from");
                    if (startDateStr != null) {
                        experience.setStartDate(parseDate(startDateStr));
                    }

                    String endDateStr = getJsonText(exp, "end_date", "endDate", "to", "duration");
                    if (endDateStr != null) {
                        experience.setEndDate(parseDate(endDateStr));
                    }

                    // Only add if we have at least some data
                    if (jobTitle != null || company != null) {
                        experiences.add(experience);
                    }
                });
            }
            profile.setExperiences(experiences);

            // Set creation timestamp
            profile.setCreatedAt(LocalDateTime.now());
            profile.setNew(true);

            // Log the profile for debugging
            log.debug("Created profile: name={}, email={}, skills={}",
                    profile.getName(), profile.getEmail(), profile.getSkills().size());

            return profile;
        });
    }

    /**
     * Helper method to get text from JSON node with multiple possible field names
     */
    private String getJsonText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode fieldNode = node.get(fieldName);
            if (fieldNode != null && !fieldNode.isNull()) {
                System.out.println(fieldNode.asText());
                return fieldNode.asText();
            }
        }
        return null;
    }

    /**
     * Parse date string to LocalDateTime (handles various formats including year-only)
     */
    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        // Clean the date string
        dateStr = dateStr.trim().replace("present", "").replace("Present", "").replace("current", "").trim();

        // Handle ranges like "2020-2022" - take the start year
        if (dateStr.contains("-") && !dateStr.toLowerCase().contains("present")) {
            dateStr = dateStr.split("-")[0].trim();
        }

        // Try different date formats
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                // For year-only format, we need to parse to YearMonth or Year first
                if (formatter.toString().equals("yyyy")) {
                    java.time.Year year = java.time.Year.parse(dateStr, formatter);
                    return year.atMonth(1).atDay(1).atStartOfDay();
                } else {
                    // For other formats, try parsing to TemporalAccessor first
                    var temporal = formatter.parse(dateStr);

                    // Check what fields are available
                    if (temporal.isSupported(java.time.temporal.ChronoField.HOUR_OF_DAY)) {
                        return LocalDateTime.from(temporal);
                    } else if (temporal.isSupported(java.time.temporal.ChronoField.MONTH_OF_YEAR)) {
                        // Has month and year but no day - use first day of month
                        int year = temporal.get(java.time.temporal.ChronoField.YEAR);
                        int month = temporal.get(java.time.temporal.ChronoField.MONTH_OF_YEAR);
                        return LocalDateTime.of(year, month, 1, 0, 0);
                    } else if (temporal.isSupported(java.time.temporal.ChronoField.YEAR)) {
                        // Only year is available
                        int year = temporal.get(java.time.temporal.ChronoField.YEAR);
                        return LocalDateTime.of(year, 1, 1, 0, 0);
                    }
                }
            } catch (DateTimeParseException ignored) {
                // Try next formatter
            }
        }

        // Try to extract year using regex
        Pattern yearPattern = Pattern.compile("\\b(19|20)\\d{2}\\b");
        var matcher = yearPattern.matcher(dateStr);
        if (matcher.find()) {
            String year = matcher.group();
            try {
                int yearInt = Integer.parseInt(year);
                return LocalDateTime.of(yearInt, 1, 1, 0, 0);
            } catch (Exception ignored) {}
        }

        log.warn("Could not parse date: {}", dateStr);
        return null;
    }
}