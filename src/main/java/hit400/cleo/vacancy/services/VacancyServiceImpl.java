package hit400.cleo.vacancy.services;

import hit400.cleo.recruitify.model.CandidateProfile;
import hit400.cleo.recruitify.repository.CandidateProfileRepository;
import hit400.cleo.recruiter.repository.CompanyRepository;
import hit400.cleo.vacancy.dtos.VacancyRequest;
import hit400.cleo.vacancy.dtos.VacancyResponse;
import hit400.cleo.vacancy.model.Vacancy;
import hit400.cleo.vacancy.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VacancyServiceImpl implements VacancyService {

    private final VacancyRepository vacancyRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CompanyRepository companyRepository;

    @Override
    public Mono<VacancyResponse> create(VacancyRequest request) {
        Vacancy vacancy = fromRequest(request, new Vacancy());
        if (vacancy.getPostedDate() == null) vacancy.setPostedDate(LocalDateTime.now());
        if (vacancy.getApplicantCount() == null) vacancy.setApplicantCount(0);
        return validateCompanyId(vacancy.getCompanyId())
                .then(vacancyRepository.save(vacancy))
                .map(this::toResponse)
                .doOnSuccess(saved -> log.info("Saved successfully: vacancy id={}", saved.getId()));
    }

    @Override
    public Mono<VacancyResponse> getById(Long id) {
        return vacancyRepository.findById(id).map(this::toResponse);
    }

    @Override
    public Flux<VacancyResponse> getAll() {
        return vacancyRepository.findAll().map(this::toResponse);
    }

    @Override
    public Flux<VacancyResponse> getByCompanyId(Integer companyId) {
        return vacancyRepository.findByCompanyIdOrderByPostedDateDesc(companyId).map(this::toResponse);
    }

    @Override
    public Flux<VacancyResponse> getRecommended(Long profileId) {
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
        return vacancyRepository.findById(id)
                .flatMap(existing -> {
                    Vacancy updated = fromRequest(request, existing);
                    return validateCompanyId(updated.getCompanyId())
                            .then(vacancyRepository.save(updated));
                })
                .map(this::toResponse)
                .doOnSuccess(saved -> log.info("Saved successfully: vacancy id={}", saved.getId()));
    }

    @Override
    public Mono<Void> delete(Long id) {
        return vacancyRepository.deleteById(id);
    }

    private Vacancy fromRequest(VacancyRequest request, Vacancy target) {
        target.setTitle(request.getTitle());
        target.setCategory(request.getCategory());
        target.setLocation(request.getLocation());
        target.setEmploymentType(request.getEmploymentType());

        if (request.getSalaryRange() != null) {
            target.setSalaryMin(request.getSalaryRange().getMin());
            target.setSalaryMax(request.getSalaryRange().getMax());
            target.setSalaryCurrency(request.getSalaryRange().getCurrency());
        } else {
            target.setSalaryMin(null);
            target.setSalaryMax(null);
            target.setSalaryCurrency(null);
        }

        target.setDescription(request.getDescription());
        target.setStatus(request.getStatus());
        target.setPostedDate(request.getPostedDate());
        target.setCompanyId(request.getCompanyId());
        target.setApplicantCount(request.getApplicantCount());

        List<String> reqs = request.getRequirements() != null ? request.getRequirements() : new ArrayList<>();
        target.setRequirements(reqs);

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

    private record ScoredVacancy(Vacancy vacancy, int score) {}
}

