package com.vyaparsetu.auth.repository;

import com.vyaparsetu.auth.entity.WebAuthnCeremony;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface WebAuthnCeremonyRepository extends JpaRepository<WebAuthnCeremony, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from WebAuthnCeremony c where c.id = :id and c.ceremonyType = :type and c.consumedAt is null")
    Optional<WebAuthnCeremony> findActiveForUpdate(
            @Param("id") String id, @Param("type") WebAuthnCeremony.Type ceremonyType);
}
