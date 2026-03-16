package hit400.cleo.recruiter.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import hit400.cleo.recruiter.dtos.CompanyRequestDTO;
import hit400.cleo.recruiter.dtos.CompanyResponseDTO;
import hit400.cleo.recruiter.model.Enums.CompanySize;
import hit400.cleo.recruiter.services.CompanyService;
import hit400.cleo.recruitify.model.enums.Industry;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CompanyController {

    private final CompanyService companyService;

    /**
     * Create a new company
     * POST /api/companies
     */
    @PostMapping
    public Mono<ResponseEntity<CompanyResponseDTO>> createCompany(
            @RequestBody CompanyRequestDTO requestDTO) {
        return companyService.createCompany(requestDTO)
                .map(company -> ResponseEntity.status(HttpStatus.CREATED).body(company));
    }

    /**
     * Get company by ID
     * GET /api/companies/{id}
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<CompanyResponseDTO>> getCompanyById(@PathVariable Integer id) {
        return companyService.getCompanyById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get all companies
     * GET /api/companies
     */
    @GetMapping
    public Flux<CompanyResponseDTO> getAllCompanies() {
        return companyService.getAllCompanies();
    }

    /**
     * Search companies
     * GET /api/companies/search?term=searchTerm
     */
    @GetMapping("/search")
    public Flux<CompanyResponseDTO> searchCompanies(
            @RequestParam String term) {
        return companyService.searchCompanies(term);
    }

    /**
     * Get companies by industry
     * GET /api/companies/filter/industry?industry=TECHNOLOGY
     */
    @GetMapping("/filter/industry")
    public Flux<CompanyResponseDTO> getByIndustry(
            @RequestParam Industry industry) {
        return companyService.getCompaniesByIndustry(industry);
    }

    /**
     * Get companies by size
     * GET /api/companies/filter/size?size=LARGE
     */
    @GetMapping("/filter/size")
    public Flux<CompanyResponseDTO> getBySize(
            @RequestParam CompanySize size) {
        return companyService.getCompaniesBySize(size);
    }

    /**
     * Update company
     * PUT /api/companies/{id}
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<CompanyResponseDTO>> updateCompany(
            @PathVariable Integer id,
            @RequestBody CompanyRequestDTO requestDTO) {
        return companyService.updateCompany(id, requestDTO)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Delete company
     * DELETE /api/companies/{id}
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteCompany(@PathVariable Integer id) {
        return companyService.deleteCompany(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
}
