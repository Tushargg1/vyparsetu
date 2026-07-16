package com.vyaparsetu.auth.repository;

import com.vyaparsetu.auth.entity.AuthenticationChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface AuthenticationChallengeRepository extends JpaRepository<AuthenticationChallenge, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AuthenticationChallenge c where c.tokenHash = :hash and c.challengeType = :type and c.consumedAt is null")
    Optional<AuthenticationChallenge> findActiveForUpdate(
            @Param("hash") String tokenHash, @Param("type") AuthenticationChallenge.Type challengeType);
}
