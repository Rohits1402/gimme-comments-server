package io.github.rohits1402.gimmecomments.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Which database rule was actually broken.
 * <p>
 * Catching DataIntegrityViolationException and assuming "duplicate" is how a missing
 * NOT NULL value came back to the widget as "Resource already exists" on Day 53. Every
 * constraint failure arrives as the same exception type, so the constraint name is the
 * only thing that says what happened. Code that claims to know has to check it.
 */
public final class ConstraintViolations {

    public static final String USER_EMAIL = "users_email_key";
    public static final String WEBSITE_URL = "websites_url_unique";
    public static final String ONE_LIKE_PER_USER = "likes_comment_id_user_id_key";
    public static final String ONE_LIVE_CODE_PER_PURPOSE = "otp_tokens_email_purpose_unique";

    private ConstraintViolations() {
    }

    public static boolean isViolationOf(DataIntegrityViolationException e, String constraint) {
        // Hibernate's ConstraintViolationException - NOT the jakarta.validation one,
        // which has the same simple name and means something entirely different.
        return e.getCause() instanceof ConstraintViolationException cause
                && constraint.equalsIgnoreCase(cause.getConstraintName());
    }
}