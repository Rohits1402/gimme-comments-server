package io.github.rohits1402.gimmecomments.repository;

import io.github.rohits1402.gimmecomments.model.Website;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WebsiteRepository extends MongoRepository<Website, String> {
    List<Website> findByUserId(String userId);

    boolean existsByWebsiteUrl(String websiteUrl);
}
