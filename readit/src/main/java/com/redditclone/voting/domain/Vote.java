package com.redditclone.voting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(
        name = "votes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_vote_voter_target",
                columnNames = {"voter_id", "target_type", "target_id"}
        )
)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Setter
    @Column(name = "voter_id", nullable = false)
    private Long voterId;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private VoteTargetType targetType;

    @Setter
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "vote_value", nullable = false, length = 20)
    private VoteValue value;

    protected Vote() {
    }

    public Vote(Long voterId, VoteTargetType targetType, Long targetId, VoteValue value) {
        this.voterId = voterId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.value = value;
    }

}
