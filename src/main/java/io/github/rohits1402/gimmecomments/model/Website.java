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
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "websites")
public class Website {

    @Id
    private String id;
    @Indexed
    private String userId;
    private String websiteName;
    private String websiteDescription = "";
    @Indexed(unique = true )
    private String websiteUrl;
    private Map<String,Object> websiteConfiguration;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
