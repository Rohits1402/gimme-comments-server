package io.github.rohits1402.gimmecomments.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "likes")
@CompoundIndex(name = "user_comment_unique", def = "{'userId': 1, 'commentId': 1}", unique = true)
public class Like {

    @Id
    private String id;

    private String userId;
    @Indexed
    private String commentId;
}