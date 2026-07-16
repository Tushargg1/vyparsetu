package com.vyaparsetu.auth.repository;

import com.vyaparsetu.auth.entity.TotpCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface TotpCredentialRepository extends JpaRepository<TotpCredential, Long> {
    boolean existsByUserIdAndEnabledTrue(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TotpCredential t where t.userId = :userId")
    Optional<TotpCredential> findByUserIdForUpdate(@Param("userId") Long userId);
}
