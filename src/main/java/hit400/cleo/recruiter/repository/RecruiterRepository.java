package hit400.cleo.recruiter.repository;

import hit400.cleo.recruiter.model.Recruiter;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface RecruiterRepository extends R2dbcRepository<Recruiter, Long> {

    Mono<Recruiter> findByEmail(String email);

    Flux<Recruiter> findByCompanyId(Integer companyId);

    @Query("""
            SELECT * FROM recruiters
            WHERE first_name ILIKE :searchTerm
               OR last_name ILIKE :searchTerm
               OR email ILIKE :searchTerm
            """)
    Flux<Recruiter> search(String searchTerm);
}

