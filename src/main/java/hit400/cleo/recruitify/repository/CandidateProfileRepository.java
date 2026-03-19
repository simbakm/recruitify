package hit400.cleo.recruitify.repository;

import hit400.cleo.recruitify.model.CandidateProfile;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CandidateProfileRepository extends R2dbcRepository<CandidateProfile, Long> {

    Mono<CandidateProfile> findByEmail(String email);

    @Query("SELECT * FROM candidate_profiles WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    Mono<CandidateProfile> findByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT * FROM candidate_profiles WHERE email = :email")
    Mono<CandidateProfile> findProfileByEmail(@Param("email") String email);
}
