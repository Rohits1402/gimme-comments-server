package io.github.rohits1402.gimmecomments.repository.jpa;

import io.github.rohits1402.gimmecomments.model.OtpPurpose;
import io.github.rohits1402.gimmecomments.model.jpa.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;


public interface OtpTokenJpaRepository extends JpaRepository<OtpToken, UUID> {

    Optional<OtpToken> findByEmailAndPurpose(String email, OtpPurpose purpose);

    void deleteByEmailAndPurpose(String email, OtpPurpose purpose);

    void deleteByExpiresAtBefore(Instant cutoff);
}