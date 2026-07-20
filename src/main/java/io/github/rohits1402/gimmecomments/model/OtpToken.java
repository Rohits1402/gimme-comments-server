package io.github.rohits1402.gimmecomments.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "otp_tokens")
public class OtpToken {

    @Id
    private String id;
    private String email;
    private String code;
    private OtpPurpose purpose;

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;
}
