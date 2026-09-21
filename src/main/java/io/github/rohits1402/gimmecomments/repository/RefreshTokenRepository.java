package io.github.rohits1402.gimmecomments.repository;

import io.github.rohits1402.gimmecomments.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * The lookup that makes the scheme revocable.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Cut off every live token descended from one login. Used tomorrow, when an
     * already-used token is presented and the safe assumption is that it was stolen.
     */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now "
            + "WHERE t.familyId = :familyId AND t.revokedAt IS NULL")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);

    /**
     * Housekeeping: expired rows can never be used again and are only clutter.
     */
    void deleteByExpiresAtBefore(Instant cutoff);
}