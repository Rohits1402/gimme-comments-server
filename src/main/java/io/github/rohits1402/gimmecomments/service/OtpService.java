package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.exception.BadRequestException;
import io.github.rohits1402.gimmecomments.model.OtpPurpose;
import io.github.rohits1402.gimmecomments.model.OtpToken;
import io.github.rohits1402.gimmecomments.repository.OtpTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final Duration OTP_VALIDITY = Duration.ofMinutes(10);
    private final OtpTokenRepository otpTokens;
    private final SecureRandom random = new SecureRandom();
    private final EmailService emailService;

    public OtpService(OtpTokenRepository otpTokens, EmailService emailService) {
        this.otpTokens = otpTokens;
        this.emailService = emailService;
    }

    public String generate(String email, OtpPurpose purpose) {
        otpTokens.deleteByEmailAndPurpose(email, purpose);
        String code = String.format("%06d", random.nextInt(1_000_000));
        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(email);
        otpToken.setPurpose(purpose);
        otpToken.setCode(code);
        otpToken.setExpiresAt(Instant.now().plus(OTP_VALIDITY));
        otpTokens.save(otpToken);

        emailService.sendOtp(email, code, purpose);
        return code;
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

    public void verifyAndConsume(String email, String code, OtpPurpose purpose) {
        verify(email, code, purpose);
        otpTokens.deleteByEmailAndPurpose(email, purpose);          // single-use
    }
}
