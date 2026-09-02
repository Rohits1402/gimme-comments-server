package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.BadRequestException;
import io.github.rohits1402.gimmecomments.exception.ConstraintViolations;
import io.github.rohits1402.gimmecomments.model.OtpPurpose;
import io.github.rohits1402.gimmecomments.model.OtpToken;
import io.github.rohits1402.gimmecomments.repository.OtpTokenRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
public class OtpService {

    private static final Duration OTP_VALIDITY = Duration.ofMinutes(10);

    private final OtpTokenRepository otpTokens;
    private final SecureRandom random = new SecureRandom();
    ApplicationEventPublisher events;

    public OtpService(OtpTokenRepository otpTokens, ApplicationEventPublisher events) {
        this.otpTokens = otpTokens;
        this.events = events;
    }

    @Transactional
    public void generate(String email, OtpPurpose purpose) {
        otpTokens.deleteByExpiresAtBefore(Instant.now());     // housekeeping, replaces the TTL index
        otpTokens.deleteByEmailAndPurpose(email, purpose);    // one live code per email and purpose

        String code = String.format("%06d", random.nextInt(1_000_000));

        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(email);
        otpToken.setPurpose(purpose);
        otpToken.setCode(code);
        otpToken.setExpiresAt(Instant.now().plus(OTP_VALIDITY));

        try {
            otpTokens.saveAndFlush(otpToken);
        } catch (DataIntegrityViolationException e) {
            if (!ConstraintViolations.isViolationOf(e, ConstraintViolations.ONE_LIVE_CODE_PER_PURPOSE)) {
                throw e;
            }
            // Another request for this address won the race and will send its own code.
            // A second email would give the reader two codes, one of which silently
            // does not work. Say nothing and let the winner's code stand.
            return;
        }

        // Deliberately not sent here. See OtpMailer - an email cannot be rolled back.
        events.publishEvent(new OtpCreated(email, code, purpose));
    }

    public void verify(String email, String code, OtpPurpose purpose) {
        OtpToken token = otpTokens.findByEmailAndPurpose(email, purpose)
                .orElseThrow(() -> new BadRequestException("OTP is invalid"));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("OTP is invalid");
        }
        if (!token.getCode().equals(code)) {
            throw new BadRequestException("OTP is invalid");
        }
    }

    @Transactional
    public void verifyAndConsume(String email, String code, OtpPurpose purpose) {
        verify(email, code, purpose);
        otpTokens.deleteByEmailAndPurpose(email, purpose);
    }
}