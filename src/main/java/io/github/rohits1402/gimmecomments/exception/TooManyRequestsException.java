package io.github.rohits1402.gimmecomments.exception;

import org.springframework.http.HttpStatus;

import java.time.Duration;

public class TooManyRequestsException extends ApiException {

    private final Duration retryAfter;

    public TooManyRequestsException(String message, Duration retryAfter) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}