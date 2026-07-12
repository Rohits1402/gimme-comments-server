package io.github.rohits1402.gimmecomments.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "likes")
public class Like {

    @Id
    private String id;

    private String userId;
    private String commentId;
}