package hit400.cleo.vacancy.repository;

import hit400.cleo.vacancy.model.Vacancy;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface VacancyRepository extends R2dbcRepository<Vacancy, Long> {

    Flux<Vacancy> findByCompanyId(Integer companyId);

    @Query("""
            SELECT * FROM vacancies
            WHERE company_id = :companyId
            ORDER BY posted_date DESC
            """)
    Flux<Vacancy> findByCompanyIdOrderByPostedDateDesc(Integer companyId);

    Flux<Vacancy> findByStatus(String status);

    Flux<Vacancy> findByCategory(String category);

    Flux<Vacancy> findByEmploymentType(String employmentType);

    @Query("""
            SELECT * FROM vacancies
            WHERE status <> :status
              AND closing_date IS NOT NULL
              AND closing_date <= :now
            """)
    Flux<Vacancy> findDueToClose(String status, java.time.LocalDateTime now);

    @Query("""
            SELECT * FROM vacancies
            WHERE title ILIKE :searchTerm
               OR category ILIKE :searchTerm
               OR location ILIKE :searchTerm
            """)
    Flux<Vacancy> search(String searchTerm);
}
