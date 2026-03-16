package hit400.cleo.candidate.repositories;

import hit400.cleo.candidate.models.Interview;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CandidateInterviewRepository extends R2dbcRepository<Interview, Long> {

    Flux<Interview> findByCandidateId(Integer candidateId);

    Flux<Interview> findByApplicationId(Integer applicationId);

    Flux<Interview> findByStatus(String status);

    @Query("""
            SELECT * FROM interviews
            WHERE candidate_name ILIKE :searchTerm
               OR position ILIKE :searchTerm
               OR location ILIKE :searchTerm
            """)
    Flux<Interview> search(String searchTerm);
}

