package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.model.OtpPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Profile("prod")
public class BrevoEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailService.class);
    private final RestClient client;
    private final String fromEmail;
    private final String fromName;

    public BrevoEmailService(@Value("${app.brevo.api-key}") String apiKey,
                             @Value("${app.mail.from}") String fromEmail,
                             @Value("${app.mail.from-name}") String fromName) {
        this.client = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", apiKey)
                .build();
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    @Async
    @Override
    public void sendOtp(String to, String otp, OtpPurpose purpose) {
        String subject = purpose == OtpPurpose.ACCOUNT_VERIFICATION
                ? "Gimme Comments - Account Verification OTP"
                : "Gimme Comments - Password Reset OTP";

        try {
            client.post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "sender", Map.of("name", fromName, "email", fromEmail),
                            "to", List.of(Map.of("email", to)),
                            "subject", subject,
                            "textContent", "Your OTP is: " + otp + "\n\nIt is valid for 10 minutes."))
                    .retrieve()
                    .toBodilessEntity();
            log.info("OTP email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", to, e);
        }
    }
}
