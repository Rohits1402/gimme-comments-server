package io.github.rohits1402.gimmecomments.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "website_id", nullable = false)
    private Website website;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "root_comment_id", nullable = false)
    private Comment rootComment;


    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<Comment> replies = new ArrayList<>();

    @Column(name = "comment_description", nullable = false)
    private String commentDescription;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Every comment belongs to a thread and stores which one; a top-level comment
     * is its own. This lives on the entity rather than in CommentService so that
     * no write path can forget it — a Comment saved straight through the
     * repository, from a test or a future importer, still joins a thread.
     * <p>
     * Assigning {@code this} is safe before the row exists: the id is generated in
     * Java, and Hibernate reads the id off the reference when it builds the INSERT.
     */
    @PrePersist
    void joinThread() {
        if (rootComment == null) {
            rootComment = parent == null ? this : parent.getRootComment();
        }
    }
}
