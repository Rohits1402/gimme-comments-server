package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.model.OtpPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailService(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Async
    @Override
    public void sendOtp(String to, String otp, OtpPurpose purpose) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(purpose == OtpPurpose.ACCOUNT_VERIFICATION
                    ? "Gimme Comments - Account Verification OTP"
                    : "Gimme Comments - Password Reset OTP");
            message.setText("Your OTP is: " + otp + "\n\nIt is valid for 10 minutes.");
            mailSender.send(message);
            log.info("OTP email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", to, e);
        }
    }
}