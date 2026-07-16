package com.vyaparsetu.auth.repository;

import com.vyaparsetu.auth.entity.PasskeyCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, Long> {
    List<PasskeyCredential> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<PasskeyCredential> findByUserHandle(byte[] userHandle);
    List<PasskeyCredential> findByCredentialId(byte[] credentialId);
    Optional<PasskeyCredential> findByCredentialIdAndUserHandle(byte[] credentialId, byte[] userHandle);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PasskeyCredential p where p.credentialId = :credentialId and p.userHandle = :userHandle")
    Optional<PasskeyCredential> findForUpdate(
            @Param("credentialId") byte[] credentialId, @Param("userHandle") byte[] userHandle);
    long countByUserId(Long userId);
}
