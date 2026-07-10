package com.redditclone.comments.ui;

import com.redditclone.comments.domain.Comment;
import com.redditclone.comments.dto.CommentDto;
import com.redditclone.comments.service.CommentService;
import com.redditclone.user.domain.User;
import com.redditclone.user.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Renders a single comment and recursively renders its replies underneath it.
 * A comment with no replies just renders as a leaf - so this same component
 * handles both flat (non-nested) and deeply nested threads without any
 * special-casing.
 */
public class CommentThreadComponent extends VerticalLayout {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");
    private static final int INDENT_PER_LEVEL_PX = 32;
    private static final int MAX_INDENT_PX = 320; // cap indentation so deep threads don't run off-screen

    private final Comment comment;
    private final CommentService commentService;
    private final UserService userService;
    private final int depth;
    private final Runnable onThreadChanged;
    private final ReplyFormCoordinator replyFormCoordinator;

    private VerticalLayout repliesContainer;
    private VerticalLayout replyFormElement;
    private boolean showingReplyForm = false;

    public CommentThreadComponent(Comment comment,
                                  int depth,
                                  CommentService commentService,
                                  UserService userService,
                                  Runnable onThreadChanged,
                                  ReplyFormCoordinator replyFormCoordinator) {
        this.comment = comment;
        this.depth = depth;
        this.commentService = commentService;
        this.userService = userService;
        this.onThreadChanged = onThreadChanged;
        this.replyFormCoordinator = replyFormCoordinator;

        setPadding(false);
        setSpacing(false);
        setWidthFull();

        // margin-left actually shifts this whole reply block rightward relative to its parent;
        // padding-left alone (the old approach) only affected inner content, not the visible
        // position of the block itself, so nesting didn't read as clearly offset.
        int indent = Math.min(depth * INDENT_PER_LEVEL_PX, MAX_INDENT_PX);
        getStyle().set("margin-left", indent + "px");
        getStyle().set("padding-left", depth > 0 ? "12px" : "0px");
        getStyle().set("border-left", depth > 0 ? "2px solid var(--lumo-contrast-10pct)" : "none");
        getStyle().set("margin-top", "8px");
        getStyle().set("width", "calc(100% - " + indent + "px)");

        add(buildHeader());
        add(buildBody());
        add(buildActionRow());

        repliesContainer = new VerticalLayout();
        repliesContainer.setPadding(false);
        repliesContainer.setSpacing(false);
        repliesContainer.setWidthFull();
        add(repliesContainer);

        loadReplies();
    }

    private HorizontalLayout buildHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setSpacing(true);
        header.setPadding(false);

        Span author = new Span(comment.getAuthor().getUsername());
        author.getStyle().set("font-weight", "600");

        Span timestamp = new Span(comment.getCreatedAt().format(TIMESTAMP_FORMAT));
        timestamp.getStyle().set("color", "var(--lumo-secondary-text-color)");
        timestamp.getStyle().set("font-size", "var(--lumo-font-size-s)");

        header.add(author, timestamp);
        return header;
    }

    private Span buildBody() {
        Span body = new Span(comment.getBody());
        body.getStyle().set("white-space", "pre-wrap");
        return body;
    }

    private HorizontalLayout buildActionRow() {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        actions.setPadding(false);

        Button reply = new Button("Reply", e -> toggleReplyForm());
        reply.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        actions.add(reply);

        User current = null;
        try {
            current = userService.getCurrentUser();
        } catch (IllegalStateException notLoggedIn) {
            // no one logged in — just skip showing the Delete button
        }

        if (current != null && current.getId().equals(comment.getAuthor().getId())) {
            Button delete = new Button("Delete", e -> deleteThisComment());
            delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            actions.add(delete);
        }

        return actions;
    }

    /**
     * Toggles this comment's own reply form. If it's about to open, tells the
     * coordinator to close whichever OTHER reply form was previously open,
     * anywhere in the tree - so only one reply box is ever visible at once.
     */
    private void toggleReplyForm() {
        if (showingReplyForm) {
            closeReplyFormInternal();
        } else {
            replyFormCoordinator.requestOpen(this);
            showingReplyForm = true;
            replyFormElement = buildReplyForm();
            // Insert right before repliesContainer (not a plain add(), which always
            // appends at the end - after every existing reply already in the thread).
            int insertIndex = indexOf(repliesContainer);
            addComponentAtIndex(insertIndex, replyFormElement);
        }
    }

    /**
     * Called by the coordinator when a DIFFERENT comment's reply form is opening.
     * Closes this one without re-notifying the coordinator (it already knows).
     */
    void closeReplyFormExternally() {
        if (showingReplyForm && replyFormElement != null) {
            remove(replyFormElement);
            replyFormElement = null;
            showingReplyForm = false;
        }
    }

    private void closeReplyFormInternal() {
        if (replyFormElement != null) {
            remove(replyFormElement);
            replyFormElement = null;
        }
        showingReplyForm = false;
        replyFormCoordinator.clear(this);
    }

    private VerticalLayout buildReplyForm() {
        VerticalLayout form = new VerticalLayout();
        form.setPadding(false);
        form.setSpacing(true);

        TextArea replyText = new TextArea();
        replyText.setPlaceholder("Write a reply...");
        replyText.setWidthFull();
        replyText.setMinHeight("80px");

        Button submit = new Button("Submit reply", e -> {
            User current;
            try {
                current = userService.getCurrentUser();
            } catch (IllegalStateException notLoggedIn) {
                Notification error = Notification.show("Please log in first.");
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            if (replyText.getValue() == null || replyText.getValue().isBlank()) {
                Notification error = Notification.show("Reply can't be empty.");
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                CommentDto dto = new CommentDto(comment.getPost().getId(), comment.getId(), replyText.getValue());
                commentService.createComment(dto.getPostId(), current.getId(), dto.getBody());
                closeReplyFormInternal();
                loadReplies();
                Notification ok = Notification.show("Reply posted.");
                ok.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalArgumentException ex) {
                Notification error = Notification.show(ex.getMessage());
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        Button cancel = new Button("Cancel", e -> toggleReplyForm());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        form.add(replyText, new HorizontalLayout(submit, cancel));
        return form;
    }

    private void deleteThisComment() {
        User current;
        try {
            current = userService.getCurrentUser();
        } catch (IllegalStateException notLoggedIn) {
            return;
        }
        try {
            commentService.deleteComment(comment.getId(), current.getId());
            Notification ok = Notification.show("Comment deleted.");
            ok.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            if (onThreadChanged != null) {
                onThreadChanged.run();
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Notification error = Notification.show(ex.getMessage());
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Fetches direct replies to this comment and rebuilds the nested list.
     * If the list comes back empty, this comment simply renders as a flat leaf -
     * that's what makes "nested and non-nested" work from the same component.
     */
    private void loadReplies() {
        repliesContainer.removeAll();
        List<Comment> replies = commentService.findReplies(comment);
        for (Comment reply : replies) {
            repliesContainer.add(new CommentThreadComponent(
                    reply, depth + 1, commentService, userService, onThreadChanged, replyFormCoordinator));
        }
    }
}