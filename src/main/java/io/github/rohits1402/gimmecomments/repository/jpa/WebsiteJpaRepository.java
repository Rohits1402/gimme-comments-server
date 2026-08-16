package io.github.rohits1402.gimmecomments.repository.jpa;

import io.github.rohits1402.gimmecomments.model.jpa.Website;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface WebsiteJpaRepository extends JpaRepository<Website, UUID> {

    List<Website> findByOwnerId(UUID ownerId);

    @Query("SELECT w FROM Website w JOIN FETCH w.owner")
    List<Website> findAllWithOwners();
}
