package hit400.cleo.vacancy.services;

import hit400.cleo.vacancy.dtos.VacancyRequest;
import hit400.cleo.vacancy.dtos.VacancyResponse;
import hit400.cleo.vacancy.model.Vacancy;
import hit400.cleo.vacancy.repository.VacancyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VacancyServiceImpl implements VacancyService {

    private final VacancyRepository vacancyRepository;

    @Override
    public Mono<VacancyResponse> create(VacancyRequest request) {
        Vacancy vacancy = fromRequest(request, new Vacancy());
        if (vacancy.getPostedDate() == null) vacancy.setPostedDate(LocalDateTime.now());
        if (vacancy.getApplicantCount() == null) vacancy.setApplicantCount(0);
        return vacancyRepository.save(vacancy).map(this::toResponse);
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
    public Mono<VacancyResponse> update(Long id, VacancyRequest request) {
        return vacancyRepository.findById(id)
                .flatMap(existing -> vacancyRepository.save(fromRequest(request, existing)))
                .map(this::toResponse);
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
}

