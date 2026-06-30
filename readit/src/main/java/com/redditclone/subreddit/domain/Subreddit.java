package com.redditclone.subreddit.domain;

import com.redditclone.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subreddits")
@Getter
@Setter
@NoArgsConstructor
public class Subreddit extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;

    public Subreddit(String name, String description, boolean isPrivate) {
        this.name = name;
        this.description = description;
        this.isPrivate = isPrivate;
    }
}
