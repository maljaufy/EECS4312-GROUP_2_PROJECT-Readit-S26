package com.redditclone.comments.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.ElementCollection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
public class Comment
{
    private UUID postId;

    @Id
    private UUID commentId;
    private UUID replyId;
    private UUID userId;
    private String content;

    @ElementCollection
    private List<UUID> upvotes;

    @ElementCollection
    private List<UUID> downvotes;
    private Timestamp createdAt;
    private Timestamp lastEditedAt;
    private Comment(UUID postId, UUID commentId,
                    UUID userId,
                    String content)
    {
        this.postId = postId;
        this.commentId = commentId;
        this.userId = userId;
        this.content = content;
        this.upvotes = new ArrayList<UUID>();
        this.downvotes = new ArrayList<UUID>();
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }
    // factory method : )
    public static Comment createComment(UUID postId,UUID userId, String content)
    {
        return new Comment(
                postId,
                UUID.randomUUID(),
                userId,
                content
        );
    }

    public static Comment reply(UUID postId, UUID userId, String content, UUID replyId)
    {
        Comment reply = createComment(postId, userId, content);
        reply.setReplyId(replyId);
        return reply;
    }
    public static Comment reconstruct(UUID postId, UUID commentId, UUID replyId, UUID userId, String content,
                                      List<UUID> upvotes, List<UUID> downvotes, Timestamp createdAt) {
        Comment c = new Comment(postId, commentId, userId, content);
        c.replyId = replyId;
        c.upvotes = upvotes;
        c.downvotes = downvotes;
        c.createdAt = createdAt;
        return c;
    }

}