package com.vyaparsetu.auth.repository;

import com.vyaparsetu.auth.entity.OtpToken;
import com.vyaparsetu.common.enums.Enums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findTopByIdentifierAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            String identifier, Enums.OtpPurpose purpose);
}
