package io.github.rohits1402.gimmecomments.config;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component("mongoHealthIndicator")
public class ApplicationMongoHealthIndicator extends AbstractHealthIndicator {

    private static final Document PING = Document.parse("{ ping: 1 }");

    private final MongoTemplate mongoTemplate;

    public ApplicationMongoHealthIndicator(MongoTemplate mongoTemplate) {
        super("MongoDB health check failed");
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        MongoDatabase db = mongoTemplate.getDb();
        db.runCommand(PING);
        builder.up().withDetail("database", db.getName());
    }
}
