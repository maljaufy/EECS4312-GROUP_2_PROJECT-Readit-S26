package com.redditclone.comments.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
public class Comment 
{ 
    private UUID commentId;
    private UUID replyId;
    private UUID userId;
    private String content;
    private List<UUID> upvotes;
    private List<UUID> downvotes;
    private Timestamp createdAt;
}