package com.redditclone.user.domain;

import com.redditclone.shared.domain.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    /*
    User domain: Main user domain: User entity
    */

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int karma = 0;

    @Column(length = 500)
    private String bio;

    @Column(length = 500)
    private String profileImageUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "users_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Role> roles = new HashSet<>();

    /**
     * Adds karma to the user's total karma score.
     * Can be positive (upvote) or negative (downvote).
     */
    public void addKarma(int delta) {
        this.karma += delta;
    }

    /**
     * Checks if the user has a specific role.
     */
    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    /**
     * Grants a role to the user.
     */
    public void addRole(Role role) {
        roles.add(role);
    }

    /**
     * Removes a role from the user.
     */
    public void removeRole(Role role) {
        roles.remove(role);
    }

    /**
     * Checks if the user is an admin.
     */
    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }

    /**
     * Checks if the user is a moderator.
     */
    public boolean isModerator() {
        return hasRole(Role.MODERATOR);
    }

    @PrePersist
    protected void onCreate() {
        if (roles.isEmpty()) {
            roles.add(Role.USER);
        }
    }

}
