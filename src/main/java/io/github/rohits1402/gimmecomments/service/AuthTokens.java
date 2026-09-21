package io.github.rohits1402.gimmecomments.service;

/**
 * What a successful sign-in or refresh hands back. The refresh token here is the only
 * copy of it that will ever exist - the database keeps a hash - so whatever receives
 * this is responsible for it.
 */
public record AuthTokens(String accessToken, String refreshToken) {
}