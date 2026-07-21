package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.model.OtpPurpose;

public interface EmailService {
    void sendOtp(String to, String otp, OtpPurpose purpose);
}