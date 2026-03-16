package hit400.cleo.recruitify.repository;


import hit400.cleo.recruitify.model.CompanyBenefit;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CompanyBenefitRepository extends R2dbcRepository<CompanyBenefit, Integer> {

    // Find all benefits for a company
    Flux<CompanyBenefit> findByCompanyId(Integer companyId);

    // Delete all benefits for a company
    Mono<Void> deleteByCompanyId(Integer companyId);
}
