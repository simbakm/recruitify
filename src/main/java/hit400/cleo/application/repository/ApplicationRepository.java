package hit400.cleo.application.repository;

import hit400.cleo.application.model.Application;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ApplicationRepository extends R2dbcRepository<Application, Long> {

    Flux<Application> findByCandidateId(Long candidateId);

    Flux<Application> findByVacancyId(Long vacancyId);
}
