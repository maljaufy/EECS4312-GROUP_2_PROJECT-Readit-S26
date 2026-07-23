package com.redditclone.comments.domain;

/**
 * Supported orderings for a post's top-level comments.
 */
public enum CommentSortOption {
    BEST("Best"),
    CONTROVERSIAL("Controversial"),
    NEWEST("Newest");

    private final String label;

    CommentSortOption(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
