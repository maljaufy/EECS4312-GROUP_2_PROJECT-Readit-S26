package com.redditclone.comments.ui;

/**
 * Shared across every CommentThreadComponent in a single post's thread.
 * When one comment opens its reply form, it registers itself here - if a
 * DIFFERENT comment's form was previously open, this tells that one to
 * close, so only one reply box is ever visible anywhere in the tree at once.
 */
public class ReplyFormCoordinator {

    private CommentThreadComponent currentlyOpen;

    public void requestOpen(CommentThreadComponent requester) {
        if (currentlyOpen != null && currentlyOpen != requester) {
            currentlyOpen.closeReplyFormExternally();
        }
        currentlyOpen = requester;
    }

    public void clear(CommentThreadComponent requester) {
        if (currentlyOpen == requester) {
            currentlyOpen = null;
        }
    }
}