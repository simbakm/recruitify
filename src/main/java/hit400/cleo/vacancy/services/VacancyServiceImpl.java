package hit400.cleo.vacancy.services;

import hit400.cleo.application.model.Application;
import hit400.cleo.application.model.enums.ApplicationStatus;
import hit400.cleo.application.repository.JobApplicationRepository;
import hit400.cleo.recruitify.model.CandidateProfile;
import hit400.cleo.recruitify.repository.CandidateProfileRepository;
import hit400.cleo.recruitify.service.EmailService;
import hit400.cleo.recruiter.repository.CompanyRepository;
import hit400.cleo.vacancy.dtos.VacancyRequest;
import hit400.cleo.vacancy.dtos.VacancyResponse;
import hit400.cleo.vacancy.model.Vacancy;
import hit400.cleo.vacancy.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VacancyServiceImpl implements VacancyService {

    private static final Logger log = LogManager.getLogger(VacancyServiceImpl.class);

    private final VacancyRepository vacancyRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CompanyRepository companyRepository;
    private final JobApplicationRepository applicationRepository;
    private final EmailService emailService;

    @Value("${app.application.scoring.threshold:0.5}")
    private double applicationScoreThreshold;

    private static final double WEIGHT_SKILLS = 0.35;
    private static final double WEIGHT_EDUCATION = 0.20;
    private static final double WEIGHT_EXPERIENCE = 0.15;
    private static final double WEIGHT_WORK_MODE = 0.10;
    private static final double WEIGHT_SALARY = 0.10;
    private static final double WEIGHT_OTHER = 0.10;

    private static final Set<String> EDUCATION_KEYWORDS = Set.of(
            "degree", "bachelor", "bsc", "ba", "master", "msc", "phd", "diploma"
    );

    @Override
    public Mono<VacancyResponse> create(VacancyRequest request) {
        log.info("Creating vacancy");
        Vacancy vacancy = fromRequest(request, new Vacancy());
        if (vacancy.getPostedDate() == null) vacancy.setPostedDate(LocalDateTime.now());
        if (vacancy.getApplicantCount() == null) vacancy.setApplicantCount(0);
        return validateCompanyId(vacancy.getCompanyId())
                .then(vacancyRepository.save(vacancy))
                .map(this::toResponse)
                .doOnSuccess(saved -> log.info("Saved successfully: vacancy id={}", saved.getId()))
                .doOnError(error -> log.error("Failed to create vacancy", error));
    }

    @Override
    public Mono<VacancyResponse> getById(Long id) {
        log.info("Fetching vacancy: id={}", id);
        return vacancyRepository.findById(id).map(this::toResponse);
    }

    @Override
    public Flux<VacancyResponse> getAll() {
        log.info("Fetching all vacancies");
        return vacancyRepository.findAll().map(this::toResponse);
    }

    @Override
    public Flux<VacancyResponse> getByCompanyId(Integer companyId) {
        return vacancyRepository.findByCompanyIdOrderByPostedDateDesc(companyId).map(this::toResponse);
    }

    @Override
    public Flux<VacancyResponse> getRecommended(Long profileId) {
        log.info("Fetching recommended vacancies: profileId={}", profileId);
        return candidateProfileRepository.findById(profileId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Candidate profile not found")))
                .flatMapMany(profile -> {
                    if (Boolean.FALSE.equals(profile.getLookingForJob())) {
                        return Flux.empty();
                    }
                    return vacancyRepository.findAll()
                            .collectList()
                            .flatMapMany(vacancies -> {
                                List<ScoredVacancy> scored = vacancies.stream()
                                        .map(vacancy -> new ScoredVacancy(vacancy, scoreVacancy(vacancy, profile)))
                                        .filter(item -> item.score() > 0)
                                        .sorted(Comparator.comparingInt(ScoredVacancy::score).reversed())
                                        .collect(Collectors.toList());
                                return Flux.fromIterable(scored)
                                        .map(ScoredVacancy::vacancy)
                                        .map(this::toResponse);
                            });
                });
    }

    @Override
    public Mono<VacancyResponse> update(Long id, VacancyRequest request) {
        log.info("Updating vacancy: id={}", id);
        return vacancyRepository.findById(id)
                .flatMap(existing -> {
                    Vacancy updated = fromRequest(request, existing);
                    return validateCompanyId(updated.getCompanyId())
                            .then(vacancyRepository.save(updated));
                })
                .map(this::toResponse)
                .doOnSuccess(saved -> log.info("Saved successfully: vacancy id={}", saved.getId()))
                .doOnError(error -> log.error("Failed to update vacancy id={}", id, error));
    }

    @Override
    public Mono<Void> closeAndScore(Long vacancyId, boolean forceClose) {
        log.info("Close and score triggered: vacancyId={}, forceClose={}", vacancyId, forceClose);
        return vacancyRepository.findById(vacancyId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Vacancy not found")))
                .flatMap(vacancy -> {
                    if (!forceClose && vacancy.getClosingDate() != null
                            && vacancy.getClosingDate().isAfter(LocalDateTime.now())) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Vacancy closing date not reached"));
                    }
                    vacancy.setStatus("CLOSED");
                    return vacancyRepository.save(vacancy);
                })
                .flatMapMany(vacancy -> applicationRepository.findByVacancyId(vacancy.getId())
                        .flatMap(application -> scoreAndUpdateApplication(application, vacancy)))
                .then()
                .doOnSuccess(ignored -> log.info("Close and score completed: vacancyId={}", vacancyId))
                .doOnError(error -> log.error("Close and score failed: vacancyId={}", vacancyId, error));
    }

    @Override
    public Mono<Void> delete(Long id) {
        log.info("Deleting vacancy: id={}", id);
        return vacancyRepository.deleteById(id)
                .doOnSuccess(ignored -> log.info("Deleted vacancy: id={}", id))
                .doOnError(error -> log.error("Failed to delete vacancy id={}", id, error));
    }

    private Vacancy fromRequest(VacancyRequest request, Vacancy target) {
        if (request.getTitle() != null) target.setTitle(request.getTitle());
        if (request.getCategory() != null) target.setCategory(request.getCategory());
        if (request.getLocation() != null) target.setLocation(request.getLocation());
        if (request.getEmploymentType() != null) target.setEmploymentType(request.getEmploymentType());

        if (request.getSalaryRange() != null) {
            target.setSalaryMin(request.getSalaryRange().getMin());
            target.setSalaryMax(request.getSalaryRange().getMax());
            target.setSalaryCurrency(request.getSalaryRange().getCurrency());
        }

        if (request.getDescription() != null) target.setDescription(request.getDescription());
        if (request.getStatus() != null) target.setStatus(request.getStatus());
        if (request.getPostedDate() != null) target.setPostedDate(request.getPostedDate());
        if (request.getClosingDate() != null) target.setClosingDate(request.getClosingDate());
        if (request.getCompanyId() != null) target.setCompanyId(request.getCompanyId());
        if (request.getApplicantCount() != null) target.setApplicantCount(request.getApplicantCount());

        if (request.getRequirements() != null) {
            List<String> reqs = new ArrayList<>(request.getRequirements());
            target.setRequirements(reqs);
        }

        return target;
    }

    private VacancyResponse toResponse(Vacancy vacancy) {
        VacancyResponse.SalaryRange salaryRange = VacancyResponse.SalaryRange.builder()
                .min(vacancy.getSalaryMin())
                .max(vacancy.getSalaryMax())
                .currency(vacancy.getSalaryCurrency())
                .build();

        List<String> requirements = vacancy.getRequirements() != null ? vacancy.getRequirements() : List.of();

        return VacancyResponse.builder()
                .id(vacancy.getId())
                .title(vacancy.getTitle())
                .category(vacancy.getCategory())
                .location(vacancy.getLocation())
                .employmentType(vacancy.getEmploymentType())
                .salaryRange(salaryRange)
                .description(vacancy.getDescription())
                .requirements(requirements)
                .status(vacancy.getStatus())
                .postedDate(vacancy.getPostedDate())
                .closingDate(vacancy.getClosingDate())
                .companyId(vacancy.getCompanyId())
                .applicantCount(vacancy.getApplicantCount())
                .build();
    }

    private Mono<Void> validateCompanyId(Integer companyId) {
        if (companyId == null) {
            return Mono.empty();
        }
        return companyRepository.existsById(companyId)
                .flatMap(exists -> exists
                        ? Mono.<Void>empty()
                        : Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Invalid companyId: " + companyId)));
    }

    private int scoreVacancy(Vacancy vacancy, CandidateProfile profile) {
        int score = 0;

        if (matchesCategory(vacancy, profile)) score += 2;
        if (matchesTitle(vacancy, profile)) score += 2;
        if (matchesWorkMode(vacancy, profile)) score += 1;
        if (matchesLocation(vacancy, profile)) score += 1;
        if (matchesSalary(vacancy, profile)) score += 2;

        List<String> profileSkills = profile.getSkills() != null ? profile.getSkills() : List.of();
        List<String> vacancyReqs = vacancy.getRequirements() != null ? vacancy.getRequirements() : List.of();
        score += countSkillMatches(profileSkills, vacancyReqs);

        return score;
    }

    private boolean matchesCategory(Vacancy vacancy, CandidateProfile profile) {
        return notBlank(profile.getDesiredCategory())
                && equalsIgnoreCase(profile.getDesiredCategory(), vacancy.getCategory());
    }

    private boolean matchesTitle(Vacancy vacancy, CandidateProfile profile) {
        return notBlank(profile.getDesiredJobTitle())
                && containsIgnoreCase(vacancy.getTitle(), profile.getDesiredJobTitle());
    }

    private boolean matchesWorkMode(Vacancy vacancy, CandidateProfile profile) {
        return notBlank(profile.getPreferredWorkMode())
                && equalsIgnoreCase(profile.getPreferredWorkMode(), vacancy.getEmploymentType());
    }

    private boolean matchesLocation(Vacancy vacancy, CandidateProfile profile) {
        return notBlank(profile.getPreferredLocation())
                && containsIgnoreCase(vacancy.getLocation(), profile.getPreferredLocation());
    }

    private boolean matchesSalary(Vacancy vacancy, CandidateProfile profile) {
        Integer desiredMin = profile.getSalaryMin();
        Integer desiredMax = profile.getSalaryMax();
        Integer vacancyMin = vacancy.getSalaryMin();
        Integer vacancyMax = vacancy.getSalaryMax();

        if (desiredMin == null && desiredMax == null) return false;
        if (vacancyMin == null && vacancyMax == null) return false;

        if (notBlank(profile.getSalaryCurrency()) && notBlank(vacancy.getSalaryCurrency())
                && !equalsIgnoreCase(profile.getSalaryCurrency(), vacancy.getSalaryCurrency())) {
            return false;
        }

        int min = desiredMin != null ? desiredMin : Integer.MIN_VALUE;
        int max = desiredMax != null ? desiredMax : Integer.MAX_VALUE;
        int vMin = vacancyMin != null ? vacancyMin : Integer.MIN_VALUE;
        int vMax = vacancyMax != null ? vacancyMax : Integer.MAX_VALUE;

        return vMax >= min && vMin <= max;
    }

    private int countSkillMatches(List<String> profileSkills, List<String> vacancyReqs) {
        int count = 0;
        for (String skill : profileSkills) {
            if (containsIgnoreCaseInList(vacancyReqs, skill)) {
                count += 1;
            }
        }
        return count;
    }

    private boolean containsIgnoreCaseInList(List<String> items, String needle) {
        if (!notBlank(needle)) return false;
        for (String item : items) {
            if (containsIgnoreCase(item, needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        if (!notBlank(haystack) || !notBlank(needle)) return false;
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (!notBlank(a) || !notBlank(b)) return false;
        return a.equalsIgnoreCase(b);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private Mono<Application> scoreAndUpdateApplication(Application application, Vacancy vacancy) {
        return candidateProfileRepository.findById(application.getCandidateId())
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Candidate profile not found for application " + application.getId())))
                .flatMap(profile -> {
                    ScoreBreakdown breakdown = computeScoreBreakdown(profile, vacancy);
                    ApplicationStatus newStatus = breakdown.total() >= applicationScoreThreshold
                            ? ApplicationStatus.Shortlisted
                            : ApplicationStatus.Rejected;

                    application.setScore(breakdown.total());
                    application.setThreshold(applicationScoreThreshold);
                    application.setStatus(newStatus);

                    log.info("Scored application id={} candidateId={} vacancyId={} total={} skills={} education={} experience={} workMode={} salary={} other={} status={}",
                            application.getId(),
                            application.getCandidateId(),
                            vacancy.getId(),
                            round(breakdown.total()),
                            round(breakdown.skills()),
                            round(breakdown.education()),
                            round(breakdown.experience()),
                            round(breakdown.workMode()),
                            round(breakdown.salary()),
                            round(breakdown.other()),
                            newStatus);

                    return applicationRepository.save(application)
                            .flatMap(saved -> sendDecisionEmail(profile, vacancy, newStatus)
                                    .thenReturn(saved));
                });
    }

    private Mono<Void> sendDecisionEmail(CandidateProfile profile, Vacancy vacancy, ApplicationStatus status) {
        if (!notBlank(profile.getEmail())) {
            return Mono.empty();
        }

        String subject = "Application Update: " + vacancy.getTitle();
        String body;
        if (status == ApplicationStatus.Shortlisted) {
            body = "Hello " + profile.getName() + ",\n\n"
                    + "Your application meets the minimum requirements for the job. "
                    + "You will be advised about the date of your interview.\n\n"
                    + "Role: " + vacancy.getTitle() + "\n"
                    + "Location: " + vacancy.getLocation() + "\n\n"
                    + "Kind regards,\nRecruitify Team";
        } else {
            body = "Hello " + profile.getName() + ",\n\n"
                    + "We are sorry to inform you that your application could not be considered "
                    + "because it does not meet the minimum requirements.\n\n"
                    + "Role: " + vacancy.getTitle() + "\n"
                    + "Location: " + vacancy.getLocation() + "\n\n"
                    + "Kind regards,\nRecruitify Team";
        }

        return emailService.sendEmail(profile.getEmail(), subject, body);
    }

    private ScoreBreakdown computeScoreBreakdown(CandidateProfile profile, Vacancy vacancy) {
        double skillsScore = scoreSkills(profile, vacancy);
        double educationScore = scoreEducation(profile, vacancy);
        double experienceScore = scoreExperience(profile, vacancy);
        double workModeScore = matchesWorkMode(vacancy, profile) ? 1.0 : 0.0;
        double salaryScore = matchesSalary(vacancy, profile) ? 1.0 : 0.0;
        double otherScore = scoreOther(profile, vacancy);

        double total =
                (WEIGHT_SKILLS * skillsScore)
                        + (WEIGHT_EDUCATION * educationScore)
                        + (WEIGHT_EXPERIENCE * experienceScore)
                        + (WEIGHT_WORK_MODE * workModeScore)
                        + (WEIGHT_SALARY * salaryScore)
                        + (WEIGHT_OTHER * otherScore);

        total = Math.min(1.0, Math.max(0.0, total));
        return new ScoreBreakdown(total, skillsScore, educationScore, experienceScore, workModeScore, salaryScore, otherScore);
    }

    private double scoreSkills(CandidateProfile profile, Vacancy vacancy) {
        List<String> requirements = vacancy.getRequirements() != null ? vacancy.getRequirements() : List.of();
        List<String> skills = profile.getSkills() != null ? profile.getSkills() : List.of();
        if (requirements.isEmpty() || skills.isEmpty()) return 0.0;

        int matches = 0;
        for (String requirement : requirements) {
            if (matchesRequirement(requirement, skills)) {
                matches++;
            }
        }
        return (double) matches / requirements.size();
    }

    private double scoreEducation(CandidateProfile profile, Vacancy vacancy) {
        if (!requiresEducation(vacancy)) return 0.0;
        return (profile.getEducations() != null && !profile.getEducations().isEmpty()) ? 1.0 : 0.0;
    }

    private double scoreExperience(CandidateProfile profile, Vacancy vacancy) {
        if (!requiresExperience(vacancy)) return 0.0;
        int count = profile.getExperiences() != null ? profile.getExperiences().size() : 0;
        if (count <= 0) return 0.0;
        if (count >= 3) return 1.0;
        return count / 3.0;
    }

    private double scoreOther(CandidateProfile profile, Vacancy vacancy) {
        int checks = 0;
        int hits = 0;

        if (matchesTitle(vacancy, profile)) {
            checks++;
            hits++;
        } else if (matchesTitleFromExperience(vacancy, profile)) {
            checks++;
            hits++;
        } else if (notBlank(profile.getDesiredJobTitle())) {
            checks++;
        }

        if (matchesCategory(vacancy, profile)) {
            checks++;
            hits++;
        } else if (notBlank(profile.getDesiredCategory())) {
            checks++;
        }

        if (matchesDescription(profile, vacancy)) {
            checks++;
            hits++;
        }

        if (checks == 0) return 0.0;
        return (double) hits / checks;
    }

    private boolean matchesRequirement(String requirement, List<String> skills) {
        if (!notBlank(requirement)) return false;
        String reqLower = requirement.toLowerCase(Locale.ROOT);
        for (String skill : skills) {
            if (!notBlank(skill)) continue;
            String skillLower = skill.toLowerCase(Locale.ROOT);
            if (reqLower.contains(skillLower) || skillLower.contains(reqLower)) {
                return true;
            }
        }
        return false;
    }

    private boolean requiresEducation(Vacancy vacancy) {
        String text = requirementText(vacancy);
        for (String keyword : EDUCATION_KEYWORDS) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private boolean requiresExperience(Vacancy vacancy) {
        String text = requirementText(vacancy);
        return text.contains("experience") || text.contains("years") || text.contains("year");
    }

    private boolean matchesTitleFromExperience(Vacancy vacancy, CandidateProfile profile) {
        if (profile.getExperiences() == null || profile.getExperiences().isEmpty()) return false;
        String title = vacancy.getTitle();
        if (!notBlank(title)) return false;
        for (CandidateProfile.Experience exp : profile.getExperiences()) {
            if (exp != null && containsIgnoreCase(exp.getJobTitle(), title)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesDescription(CandidateProfile profile, Vacancy vacancy) {
        if (!notBlank(profile.getObjectives()) || !notBlank(vacancy.getDescription())) return false;
        return containsIgnoreCase(vacancy.getDescription(), profile.getObjectives())
                || containsIgnoreCase(profile.getObjectives(), vacancy.getDescription());
    }

    private String requirementText(Vacancy vacancy) {
        StringBuilder builder = new StringBuilder();
        if (vacancy.getRequirements() != null) {
            for (String req : vacancy.getRequirements()) {
                if (notBlank(req)) builder.append(req.toLowerCase(Locale.ROOT)).append(' ');
            }
        }
        if (notBlank(vacancy.getDescription())) {
            builder.append(vacancy.getDescription().toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private record ScoreBreakdown(
            double total,
            double skills,
            double education,
            double experience,
            double workMode,
            double salary,
            double other
    ) {}

    private record ScoredVacancy(Vacancy vacancy, int score) {}
}

