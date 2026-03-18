package hit400.cleo.candidate.repositories;

import hit400.cleo.candidate.models.Application;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ApplicationRepository extends R2dbcRepository<Application, Long> {

    Flux<Application> findByVacancyId(Long vacancyId);

    Flux<Application> findByCandidateId(Long candidateId);

    Flux<Application> findByStatus(String status);

    @Query("""
            SELECT * FROM applications
            WHERE (:vacancyId IS NULL OR vacancy_id = :vacancyId)
              AND (:candidateId IS NULL OR candidate_id = :candidateId)
              AND (:status IS NULL OR status = :status)
            ORDER BY applied_date DESC, id DESC
            """)
    Flux<Application> findFiltered(Long vacancyId, Long candidateId, String status);
}
