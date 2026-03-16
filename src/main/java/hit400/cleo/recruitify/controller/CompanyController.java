package hit400.cleo.recruitify.controller;

import hit400.cleo.recruitify.dto.CompanyRequestDTO;
import hit400.cleo.recruitify.dto.CompanyResponseDTO;
import hit400.cleo.recruitify.model.enums.CompanySize;
import hit400.cleo.recruitify.model.enums.Industry;
import hit400.cleo.recruitify.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
