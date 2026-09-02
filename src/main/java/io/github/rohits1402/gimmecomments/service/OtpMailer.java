package io.github.rohits1402.gimmecomments.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the code only once the database has agreed to keep it.
 * <p>
 * Sending from inside the transaction meant a rollback threw the code away while the
 * email was already on its way, so the reader received a code that could never work
 * and was told "OTP is invalid" with nothing in the logs to explain it.
 * <p>
 * AFTER_COMMIT is the whole point. It also means this listener never runs when there
 * is no transaction at all - which is silent, so OtpService.generate must stay
 *
 * @Transactional or the emails simply stop.
 */
@Component
class OtpMailer {

    private final EmailService emailService;

    OtpMailer(EmailService emailService) {
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void send(OtpCreated event) {
        // sendOtp is @Async, so this still leaves the request thread immediately.
        emailService.sendOtp(event.email(), event.code(), event.purpose());
    }
}