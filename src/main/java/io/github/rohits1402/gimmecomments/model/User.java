package io.github.rohits1402.gimmecomments.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;


@Getter
@Setter
@NoArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;                      // Mongo ObjectId, as String — Spring Data converts

    private String name;
    private Gender gender = Gender.MALE;    // old schema's default
    private String birthday = "";
    private String profileImage = "";

    @Indexed(unique = true)
    private String email;

    private String password;                // will hold a bcrypt HASH from Day 15 on — never plaintext

    private boolean emailVerified = false;
    private boolean accountActive = true;

    @CreatedDate
    private Instant createdAt;              // { timestamps: true }, the Spring way
    @LastModifiedDate
    private Instant updatedAt;
}