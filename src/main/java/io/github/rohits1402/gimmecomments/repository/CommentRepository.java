package io.github.rohits1402.gimmecomments.repository;

import io.github.rohits1402.gimmecomments.model.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Limit;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {


    @Query("""
            SELECT new io.github.rohits1402.gimmecomments.repository.CommentCount(c.website.id, COUNT(c))
            FROM Comment c
            WHERE c.website.id IN :websiteIds
            GROUP BY c.website.id
            """)
    List<CommentCount> countByWebsiteIds(@Param("websiteIds") Collection<UUID> websiteIds);

    /**
     * The newest comments anywhere on the caller's websites.
     * <p>
     * Both fetches are single-valued, so the limit is applied by the database. A
     * collection fetch here would make Hibernate load every row and page in memory.
     * The author fetch is a LEFT join because an author can be null once they delete
     * their account; the website fetch is an inner join because that column is NOT NULL.
     */
    @Query("""
            SELECT c FROM Comment c
            JOIN FETCH c.website w
            LEFT JOIN FETCH c.author
            WHERE w.owner.id = :ownerId
            ORDER BY c.createdAt DESC
            """)
    List<Comment> findRecentForOwner(@Param("ownerId") UUID ownerId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.website.owner.id = :ownerId")
    long countForOwner(@Param("ownerId") UUID ownerId);

    @Query("SELECT COUNT(DISTINCT c.author.id) FROM Comment c WHERE c.website.owner.id = :ownerId")
    long countDistinctAuthorsForOwner(@Param("ownerId") UUID ownerId);

    /**
     * Just the timestamps, grouped into days by the caller. Grouping in SQL would mean
     * a date-truncation function, and those differ between databases — the H2-in-tests,
     * PostgreSQL-in-production split is exactly where that bites. Two weeks of instants
     * is a small list.
     */
    @Query("SELECT c.createdAt FROM Comment c WHERE c.website.owner.id = :ownerId AND c.createdAt >= :since")
    List<Instant> createdAtForOwnerSince(@Param("ownerId") UUID ownerId, @Param("since") Instant since);

    /**
     * The first page of threads: the newest root comments on a website.
     * <p>
     * The author fetch is single-valued, so the database applies the limit. A
     * fetch of c.replies here would make Hibernate read every row and cut the
     * page in memory — the same trap as findRecentForOwner.
     */
    @Query("""
            SELECT c FROM Comment c
            LEFT JOIN FETCH c.author
            WHERE c.website.id = :websiteId AND c.parent IS NULL
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    List<Comment> findRoots(@Param("websiteId") UUID websiteId, Limit limit);

    /**
     * The next page: the newest root comments strictly older than the cursor.
     */
    @Query("""
            SELECT c FROM Comment c
            LEFT JOIN FETCH c.author
            WHERE c.website.id = :websiteId AND c.parent IS NULL
              AND (c.createdAt < :beforeAt
                   OR (c.createdAt = :beforeAt AND c.id < :beforeId))
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    List<Comment> findRootsBefore(@Param("websiteId") UUID websiteId,
                                  @Param("beforeAt") Instant beforeAt,
                                  @Param("beforeId") UUID beforeId,
                                  Limit limit);

    /**
     * Every reply belonging to the given threads, at any depth, in one query.
     * Roots point at themselves, so they match :rootIds too — the parent check
     * removes them, since the caller already holds them.
     */
    @Query("""
            SELECT c FROM Comment c
            LEFT JOIN FETCH c.author
            WHERE c.rootComment.id IN :rootIds AND c.parent IS NOT NULL
            ORDER BY c.createdAt ASC
            """)
    List<Comment> findRepliesForRoots(@Param("rootIds") Collection<UUID> rootIds);

    long countByWebsiteId(UUID websiteId);


}
