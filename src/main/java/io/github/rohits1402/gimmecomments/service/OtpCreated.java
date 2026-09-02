package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.model.OtpPurpose;

/**
 * A one-time code that the database has been asked to keep. Published inside the
 * transaction and acted on after it commits.
 * <p>
 * It carries the code itself, so it must never be logged.
 */
record OtpCreated(String email, String code, OtpPurpose purpose) {
}