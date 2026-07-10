package com.redditclone.comments.domain;

import com.redditclone.shared.domain.BaseEntity;
import com.redditclone.posts.domain.Post;
import com.redditclone.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "comments")
@Getter
@Setter
public class Comment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 5000)
    private String body;

    @Column(name = "vote_score")
    private int voteScore = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    protected Comment() {
    }

    public Comment(Post post, User author, String body) {
        this.post = post;
        this.author = author;
        this.body = body;
    }

    public Long getPostId() {
        return post == null ? null : post.getId();
    }

    public void setPostId(Long postId) {
        Post postReference = new Post();
        postReference.setId(postId);
        this.post = postReference;
    }

    public Long getAuthorId() {
        return author == null ? null : author.getId();
    }

    public void setAuthorId(Long authorId) {
        User authorReference = new User();
        authorReference.setId(authorId);
        this.author = authorReference;
    }
}
