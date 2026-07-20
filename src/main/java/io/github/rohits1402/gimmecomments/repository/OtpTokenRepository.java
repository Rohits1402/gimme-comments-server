package io.github.rohits1402.gimmecomments.repository;

import io.github.rohits1402.gimmecomments.model.OtpPurpose;
import io.github.rohits1402.gimmecomments.model.OtpToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OtpTokenRepository extends MongoRepository<OtpToken, String> {
    Optional<OtpToken> findByEmailAndPurpose(String email, OtpPurpose purpose);

    void deleteByEmailAndPurpose(String email, OtpPurpose purpose);
}
