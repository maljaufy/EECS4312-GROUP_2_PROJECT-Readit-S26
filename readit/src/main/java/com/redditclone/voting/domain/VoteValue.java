package com.redditclone.voting.domain;

public enum VoteValue {
    UPVOTE(1),
    DOWNVOTE(-1);

    private final int karmaDelta;

    VoteValue(int karmaDelta) {
        this.karmaDelta = karmaDelta;
    }

    public int getKarmaDelta() {
        return karmaDelta;
    }
}
