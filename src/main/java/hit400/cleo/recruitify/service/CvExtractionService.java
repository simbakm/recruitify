package hit400.cleo.recruitify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hit400.cleo.recruitify.dto.CvUploadRequest;
import hit400.cleo.recruitify.dto.CvUpdateResult;
import hit400.cleo.recruitify.model.CandidateProfile;
import hit400.cleo.recruitify.repository.CandidateProfileRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class CvExtractionService {

    private static final Logger log = LogManager.getLogger(CvExtractionService.class);

    private final CandidateProfileRepository candidateProfileRepository;
    private static final String CV_STORAGE_DIR = "uploads/cv/";
    private final ObjectMapper objectMapper;

    // Python microservice URL
    @Value("${app.python.microservice.url}")
    private String pythonMicroserviceUrl;


    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(CV_STORAGE_DIR));
            log.info("CV upload directory created/verified: {}", CV_STORAGE_DIR);
        } catch (IOException e) {
            log.error("Failed to create CV upload directory", e);
            // Depending on your needs, you might want to throw a runtime exception
            // to prevent the application from starting if the directory cannot be created.
            throw new RuntimeException("Could not initialize CV storage directory", e);
        }
    }

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

    /**
     * Main flow: save the CV file, call Python microservice, parse response, save profile.
     */
    public Mono<CandidateProfile> extractAndSaveProfile(CvUploadRequest request) {
        log.info("Starting CV extraction");
        return saveCvFile(request.cvFile())
                .flatMap(filePath ->
                        callPythonMicroservice(filePath)
                                .flatMap(this::parseStructuredData)
                                .map(profile -> {
                                    profile.setCvFilePath(filePath); // Set the file path
                                    return profile;
                                })
                                .flatMap(this::upsertByEmail)
                )
                .doOnSuccess(profile -> log.info("Saved successfully: profile id={} email={}", profile.getId(), profile.getEmail()))
                .doOnError(error -> log.error("Failed to process CV", error));
    }

    /**
     * Flow for existing account: save CV, parse it, merge into existing profile by id.
     */
    public Mono<CvUpdateResult> extractAndUpdateProfile(Long profileId, FilePart cvFilePart) {
        log.info("Starting CV extraction for profileId={}", profileId);
        if (cvFilePart == null) {
            return Mono.error(new IllegalArgumentException("cvFile is required"));
        }

        return candidateProfileRepository.findById(profileId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Candidate profile not found")))
                .flatMap(existing ->
                        saveCvFile(cvFilePart)
                                .flatMap(filePath ->
                                        callPythonMicroservice(filePath)
                                                .flatMap(this::parseStructuredData)
                                                .flatMap(parsed -> {
                                                    parsed.setCvFilePath(filePath);
                                                    List<String> warnings = buildMismatchWarnings(existing, parsed);
                                                    CandidateProfile merged = mergeParsedIntoExisting(existing, parsed);
                                                    return candidateProfileRepository.save(merged)
                                                            .map(saved -> new CvUpdateResult(saved, warnings));
                                                })
                                )
                )
                .doOnSuccess(result -> log.info("Updated successfully: profile id={} email={}",
                        result.profile().getId(), result.profile().getEmail()))
                .doOnError(error -> log.error("Failed to process CV for profileId={}", profileId, error));
    }

    /**
     * Save the uploaded file to disk using streaming (no memory buffering).
     */
    private Mono<String> saveCvFile(FilePart filePart) {
        String filename = System.currentTimeMillis() + "_" + filePart.filename();
        Path path = Paths.get(CV_STORAGE_DIR + filename);
        return filePart.transferTo(path)
                .thenReturn(path.toString())
                .doOnSuccess(saved -> log.info("Saved CV file: {}", saved))
                .doOnError(error -> log.error("Failed to save CV file", error));
    }

    /**
     * Call the Python microservice, streaming the already-saved file.
     */
    private Mono<JsonNode> callPythonMicroservice(String filePath) {
        Path path = Paths.get(filePath);
        Resource resource = new FileSystemResource(path);

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", resource)
                .filename(path.getFileName().toString())
                .contentType(MediaType.APPLICATION_PDF);

        log.info("Sending PDF to Python microservice: {}", path.getFileName());

        WebClient webClient = WebClient.builder()
                .baseUrl(pythonMicroserviceUrl)
                .build();

        return webClient.post()
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        return objectMapper.readTree(response);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse microservice response", e);
                    }
                })
                .doOnSuccess(json -> log.info("Successfully received parsed data from microservice"))
                .doOnError(error -> log.error("Error calling Python microservice", error));
    }

    /**
     * Parse the JSON response from the microservice into a CandidateProfile.
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
                // todo: what to do if the cv does not contain an email
            }
            profile.setEmail(email);

            // Phone
            profile.setPhone(getJsonText(dataNode, "phone"));

            // Address
            profile.setAddress(getJsonText(dataNode, "location", "address"));

            // Objectives
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
     * Helper method to get text from JSON node with multiple possible field names.
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
     * Parse date string to LocalDateTime (handles various formats including year-only).
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

    /**
     * Retrieve a CV file as a Resource for streaming to the client.
     */
    public Mono<Resource> getCvFile(Long profileId) {
        log.info("Fetching CV file: profileId={}", profileId);
        return candidateProfileRepository.findById(profileId)
                .map(profile -> new FileSystemResource(profile.getCvFilePath()));
    }

    private Mono<CandidateProfile> upsertByEmail(CandidateProfile incoming) {
        log.info("Upsert profile by email={}", incoming.getEmail());
        if (incoming.getEmail() == null || incoming.getEmail().isBlank()) {
            if (incoming.getCreatedAt() == null) incoming.setCreatedAt(LocalDateTime.now());
            if (incoming.getName() == null || incoming.getName().isBlank()) {
                return Mono.error(new IllegalArgumentException("name is required"));
            }
            incoming.setNew(true);
            return candidateProfileRepository.save(incoming);
        }

        return candidateProfileRepository.findByEmail(incoming.getEmail())
                .flatMap(existing -> {
                    incoming.setId(existing.getId());
                    incoming.setNew(false);
                    if (incoming.getName() == null || incoming.getName().isBlank()) {
                        incoming.setName(existing.getName());
                    }
                    if (incoming.getCreatedAt() == null && existing.getCreatedAt() != null) {
                        incoming.setCreatedAt(existing.getCreatedAt());
                    }
                    return candidateProfileRepository.save(incoming);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    if (incoming.getCreatedAt() == null) incoming.setCreatedAt(LocalDateTime.now());
                    if (incoming.getName() == null || incoming.getName().isBlank()) {
                        return Mono.error(new IllegalArgumentException("name is required"));
                    }
                    incoming.setNew(true);
                    return candidateProfileRepository.save(incoming)
                            .doOnSuccess(saved -> log.info("Inserted profile without email: id={}", saved.getId()))
                            .doOnError(error -> log.error("Failed to insert profile without email", error));
                }));
    }

    private CandidateProfile mergeParsedIntoExisting(CandidateProfile existing, CandidateProfile parsed) {
        existing.setNew(false);
        if (existing.getCreatedAt() == null) {
            existing.setCreatedAt(LocalDateTime.now());
        }
        existing.setCvFilePath(parsed.getCvFilePath());

        if (isBlank(existing.getName()) && !isBlank(parsed.getName())) {
            existing.setName(parsed.getName());
        }
        if (isBlank(existing.getEmail()) && !isBlank(parsed.getEmail())) {
            existing.setEmail(parsed.getEmail());
        }
        if (isBlank(existing.getPhone()) && !isBlank(parsed.getPhone())) {
            existing.setPhone(parsed.getPhone());
        }
        if (isBlank(existing.getAddress()) && !isBlank(parsed.getAddress())) {
            existing.setAddress(parsed.getAddress());
        }

        if (!isBlank(parsed.getObjectives())) {
            existing.setObjectives(parsed.getObjectives());
        }
        if (parsed.getSkills() != null && !parsed.getSkills().isEmpty()) {
            existing.setSkills(parsed.getSkills());
        }
        if (parsed.getExperiences() != null && !parsed.getExperiences().isEmpty()) {
            existing.setExperiences(parsed.getExperiences());
        }
        if (parsed.getEducations() != null && !parsed.getEducations().isEmpty()) {
            existing.setEducations(parsed.getEducations());
        }

        return existing;
    }

    private List<String> buildMismatchWarnings(CandidateProfile existing, CandidateProfile parsed) {
        List<String> warnings = new ArrayList<>();

        if (!isBlank(existing.getName()) && !isBlank(parsed.getName())) {
            String existingName = normalizeName(existing.getName());
            String parsedName = normalizeName(parsed.getName());
            if (!existingName.equals(parsedName)) {
                warnings.add("Name mismatch between signup and CV");
                log.warn("CV name mismatch for profileId={}: signup='{}' cv='{}'",
                        existing.getId(), existing.getName(), parsed.getName());
            }
        }

        if (!isBlank(existing.getEmail()) && !isBlank(parsed.getEmail())
                && !existing.getEmail().equalsIgnoreCase(parsed.getEmail())) {
            warnings.add("Email mismatch between signup and CV");
            log.warn("CV email mismatch for profileId={}: signup='{}' cv='{}'",
                    existing.getId(), existing.getEmail(), parsed.getEmail());
        }

        if (!isBlank(existing.getPhone()) && !isBlank(parsed.getPhone())) {
            String existingPhone = normalizePhone(existing.getPhone());
            String parsedPhone = normalizePhone(parsed.getPhone());
            if (!existingPhone.equals(parsedPhone)) {
                warnings.add("Phone mismatch between signup and CV");
                log.warn("CV phone mismatch for profileId={}: signup='{}' cv='{}'",
                        existing.getId(), existing.getPhone(), parsed.getPhone());
            }
        }

        return warnings;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}
