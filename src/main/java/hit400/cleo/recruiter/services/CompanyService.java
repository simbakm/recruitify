package hit400.cleo.recruiter.services;

import com.fasterxml.jackson.databind.ObjectMapper;

import hit400.cleo.recruiter.dtos.CompanyRequestDTO;
import hit400.cleo.recruiter.dtos.CompanyResponseDTO;
import hit400.cleo.recruiter.model.Company;
import hit400.cleo.recruiter.model.CompanyBenefit;
import hit400.cleo.recruiter.model.Enums.CompanySize;
import hit400.cleo.recruiter.repository.CompanyBenefitRepository;
import hit400.cleo.recruiter.repository.CompanyRepository;
import hit400.cleo.recruitify.model.enums.Industry;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private static final Logger log = LogManager.getLogger(CompanyService.class);

    private final CompanyRepository companyRepository;
    private final CompanyBenefitRepository companyBenefitRepository;
    private final ObjectMapper objectMapper;

    /**
     * Create a new company
     */
    public Mono<CompanyResponseDTO> createCompany(CompanyRequestDTO requestDTO) {
        log.info("Creating company");
        return Mono.fromCallable(() -> Company.builder()
                .name(requestDTO.getName())
                .logoUrl(requestDTO.getLogoUrl())
                .industry(requestDTO.getIndustry())
                .size(requestDTO.getSize())
                .website(requestDTO.getWebsite())
                .location(requestDTO.getLocation())
                .description(requestDTO.getDescription())
                .culture(requestDTO.getCulture())
                .socialLinks(objectMapper.writeValueAsString(requestDTO.getSocialLinks()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build())
                .flatMap(company -> companyRepository.save(company)
                        .flatMap(savedCompany -> saveBenefits(savedCompany, requestDTO.getBenefits())
                                .then(Mono.just(savedCompany))
                        )
                )
                .flatMap(this::companyToResponseDTO)
                .doOnSuccess(dto -> log.info("Saved successfully: company id={}", dto.getId()))
                .doOnError(error -> log.error("Failed to create company", error));
    }

    /**
     * Get company by ID
     */
    public Mono<CompanyResponseDTO> getCompanyById(Integer id) {
        log.info("Fetching company: id={}", id);
        return companyRepository.findById(id)
                .flatMap(this::companyToResponseDTO);
    }

    /**
     * Get all companies
     */
    public Flux<CompanyResponseDTO> getAllCompanies() {
        log.info("Fetching all companies");
        return companyRepository.findAll()
                .flatMap(this::companyToResponseDTO);
    }

    /**
     * Search companies by term
     */
    public Flux<CompanyResponseDTO> searchCompanies(String searchTerm) {
        log.info("Searching companies: term={}", searchTerm);
        return companyRepository.searchCompanies("%" + searchTerm + "%")
                .flatMap(this::companyToResponseDTO);
    }

    /**
     * Get companies by industry
     */
    public Flux<CompanyResponseDTO> getCompaniesByIndustry(Industry industry) {
        log.info("Fetching companies by industry={}", industry);
        return companyRepository.findByIndustry(industry)
                .flatMap(this::companyToResponseDTO);
    }

    /**
     * Get companies by size
     */
    public Flux<CompanyResponseDTO> getCompaniesBySize(CompanySize size) {
        log.info("Fetching companies by size={}", size);
        return companyRepository.findBySize(size)
                .flatMap(this::companyToResponseDTO);
    }

    /**
     * Update company
     */
    public Mono<CompanyResponseDTO> updateCompany(Integer id, CompanyRequestDTO requestDTO) {
        log.info("Updating company: id={}", id);

        return companyRepository.findById(id)
                .flatMap(company -> {

                    company.setName(requestDTO.getName());
                    company.setLogoUrl(requestDTO.getLogoUrl());
                    company.setIndustry(requestDTO.getIndustry());
                    company.setSize(requestDTO.getSize());
                    company.setWebsite(requestDTO.getWebsite());
                    company.setLocation(requestDTO.getLocation());
                    company.setDescription(requestDTO.getDescription());
                    company.setCulture(requestDTO.getCulture());
                    company.setUpdatedAt(LocalDateTime.now());

                    try {
                        company.setSocialLinks(
                                objectMapper.writeValueAsString(requestDTO.getSocialLinks())
                        );
                    } catch (Exception e) {
                        return Mono.error(e);
                    }

                    return companyBenefitRepository.deleteByCompanyId(id)
                            .then(companyRepository.save(company))
                            .flatMap(savedCompany ->
                                    saveBenefits(savedCompany, requestDTO.getBenefits())
                                            .thenReturn(savedCompany)
                            );
                })
                .flatMap(this::companyToResponseDTO)
                .doOnSuccess(dto -> log.info("Saved successfully: company id={}", dto.getId()))
                .doOnError(error -> log.error("Failed to update company id={}", id, error));
    }

    /**
     * Delete company
     */
    public Mono<Void> deleteCompany(Integer id) {
        log.info("Deleting company: id={}", id);
        return companyBenefitRepository.deleteByCompanyId(id)
                .then(companyRepository.deleteById(id))
                .doOnSuccess(ignored -> log.info("Deleted company: id={}", id))
                .doOnError(error -> log.error("Failed to delete company id={}", id, error));
    }

    /**
     * Helper method: Save benefits for a company
     */
    private Mono<Void> saveBenefits(Company company, List<String> benefits) {
        if (benefits == null || benefits.isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(benefits)
                .map(benefit -> CompanyBenefit.builder()
                        .companyId(company.getId())
                        .benefit(benefit)
                        .createdAt(LocalDateTime.now())
                        .build()
                )
                .flatMap(companyBenefitRepository::save)
                .then();
    }

    /**
     * Helper method: Convert Company to CompanyResponseDTO
     */
    private Mono<CompanyResponseDTO> companyToResponseDTO(Company company) {

        return companyBenefitRepository.findByCompanyId(company.getId())
                .map(CompanyBenefit::getBenefit)
                .collectList()
                .handle((benefits, sink) -> {

                    Map<String,String> socialLinks = Map.of();

                    try {
                        if (company.getSocialLinks() != null) {
                            socialLinks = objectMapper.readValue(
                                    company.getSocialLinks(),
                                    new com.fasterxml.jackson.core.type.TypeReference<>() {
                                    }
                            );
                        }
                    } catch (Exception e) {
                        sink.error(new RuntimeException(e));
                        return;
                    }

                    sink.next(CompanyResponseDTO.builder()
                            .id(company.getId())
                            .name(company.getName())
                            .logoUrl(company.getLogoUrl())
                            .industry(company.getIndustry())
                            .size(company.getSize())
                            .website(company.getWebsite())
                            .location(company.getLocation())
                            .description(company.getDescription())
                            .culture(company.getCulture())
                            .benefits(benefits)
                            .socialLinks(socialLinks)
                            .createdAt(company.getCreatedAt())
                            .updatedAt(company.getUpdatedAt())
                            .build());
                });
    }
}

