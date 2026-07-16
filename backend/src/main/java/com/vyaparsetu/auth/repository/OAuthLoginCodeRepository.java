package com.vyaparsetu.auth.repository;

import com.vyaparsetu.auth.entity.OAuthLoginCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OAuthLoginCodeRepository extends JpaRepository<OAuthLoginCode, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OAuthLoginCode c where c.codeHash = :hash and c.consumedAt is null")
    Optional<OAuthLoginCode> findUsableForUpdate(@Param("hash") String hash);
}