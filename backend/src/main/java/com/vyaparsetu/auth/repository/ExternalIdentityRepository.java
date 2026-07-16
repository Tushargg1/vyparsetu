package com.vyaparsetu.auth.repository;

import com.vyaparsetu.auth.entity.ExternalIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, Long> {
    Optional<ExternalIdentity> findByProviderAndProviderSubject(String provider, String providerSubject);
}