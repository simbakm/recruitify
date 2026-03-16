package hit400.cleo.recruitify.repository;


import hit400.cleo.recruitify.model.Company;
import hit400.cleo.recruitify.model.enums.CompanySize;
import hit400.cleo.recruitify.model.enums.Industry;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Repository
public interface CompanyRepository extends R2dbcRepository<Company, Integer> {

    // Find company by name
    Mono<Company> findByName(String name);

    // Find companies by industry
    Flux<Company> findByIndustry(Industry industry);

    // Find companies by size
    Flux<Company> findBySize(CompanySize size);

    // Custom query: Search companies by name or location
    @Query("SELECT * FROM companies WHERE name ILIKE :searchTerm OR location ILIKE :searchTerm")
    Flux<Company> searchCompanies(String searchTerm);

    // Find all companies
    Flux<Company> findAllBy();
}

