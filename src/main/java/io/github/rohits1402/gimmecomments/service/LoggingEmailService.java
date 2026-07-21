package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.model.OtpPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Profile("dev")
public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    @Async
    @Override
    public void sendOtp(String to, String otp, OtpPurpose purpose) {
        log.info("[DEV EMAIL] to={} purpose={} otp={}", to, purpose, otp);
    }
}