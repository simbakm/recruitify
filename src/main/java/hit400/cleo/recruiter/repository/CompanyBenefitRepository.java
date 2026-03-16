package hit400.cleo.recruiter.repository;


import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import hit400.cleo.recruiter.model.CompanyBenefit;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CompanyBenefitRepository extends R2dbcRepository<CompanyBenefit, Integer> {

    // Find all benefits for a company
    Flux<CompanyBenefit> findByCompanyId(Integer companyId);

    // Delete all benefits for a company
    Mono<Void> deleteByCompanyId(Integer companyId);
}
