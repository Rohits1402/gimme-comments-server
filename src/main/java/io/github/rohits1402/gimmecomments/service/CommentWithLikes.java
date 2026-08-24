package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.model.Comment;

public record CommentWithLikes(Comment comment, long likedBy, boolean iLiked) {
}