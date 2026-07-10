package com.redditclone.posts.domain;

import com.redditclone.shared.domain.BaseEntity;
import com.redditclone.subreddit.domain.Subreddit;
import com.redditclone.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
public class Post extends BaseEntity {

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 10000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subreddit_id", nullable = false)
    private Subreddit subreddit;

    @Column(name = "vote_score")
    private int voteScore = 0;

    public Post(String title, String content, User author, Subreddit subreddit) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.subreddit = subreddit;
    }
}
