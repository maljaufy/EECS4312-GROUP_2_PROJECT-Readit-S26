package com.redditclone.comments.ui;

import com.redditclone.comments.domain.Comment;
import com.redditclone.comments.dto.CommentDto;
import com.redditclone.comments.service.CommentService;
import com.redditclone.shared.security.UserSession;
import com.redditclone.user.service.UserService;
import com.redditclone.voting.domain.VoteValue;
import com.redditclone.voting.dto.VoteResult;
import com.redditclone.voting.service.VoteService;
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
 * NOTE: identity is read via {@link UserSession#currentUserId(com.vaadin.flow.component.UI)},
 * which checks both the VaadinSession and the underlying HttpSession's
 * "userId" attribute (set at login/register in AuthenticationSessionService /
 * UserService, via UserSession.establish()).
 */
public class CommentThreadComponent extends VerticalLayout {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");
    private static final int INDENT_PER_LEVEL_PX = 32;
    private static final int MAX_INDENT_PX = 320; // cap indentation so deep threads don't run off-screen

    private final Comment comment;
    private final CommentService commentService;
    private final UserService userService;
    private final VoteService voteService;
    private final UserSession userSession;
    private final int depth;
    private final Runnable onThreadChanged;
    private final ReplyFormCoordinator replyFormCoordinator;

    private final VerticalLayout repliesContainer;
    private VerticalLayout replyFormElement;
    private boolean showingReplyForm = false;

    public CommentThreadComponent(Comment comment,
                                  int depth,
                                  CommentService commentService,
                                  UserService userService,
                                  VoteService voteService,
                                  UserSession userSession,
                                  Runnable onThreadChanged,
                                  ReplyFormCoordinator replyFormCoordinator) {
        this.comment = comment;
        this.depth = depth;
        this.commentService = commentService;
        this.userService = userService;
        this.voteService = voteService;
        this.userSession = userSession;
        this.onThreadChanged = onThreadChanged;
        this.replyFormCoordinator = replyFormCoordinator;

        setPadding(false);
        setSpacing(false);
        setWidthFull();

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

    /**
     * Returns the logged-in user's ID, or null if nobody's logged in.
     * Delegates to {@link UserSession#currentUserId}, which is the same
     * source of truth used by UserService.getCurrentUser() elsewhere.
     */
    private Long getCurrentUserId() {
        return getUI().map(userSession::currentUserId).orElse(null);
    }

    private HorizontalLayout buildHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setSpacing(true);
        header.setPadding(false);

        Span author = new Span(comment.getAuthor().getUsername());
        author.getStyle()
                .set("font-weight", "600")
                .set("color", "#0079D3")
                .set("cursor", "pointer");
        author.getElement().addEventListener("mouseover", e ->
                author.getStyle().set("text-decoration", "underline"));
        author.getElement().addEventListener("mouseout", e ->
                author.getStyle().set("text-decoration", "none"));
        author.addClickListener(event ->
                getUI().ifPresent(ui -> ui.navigate("profile/" + comment.getAuthor().getUsername())));

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

        Span score = new Span(String.valueOf(voteService.getCommentScore(comment.getId())));
        score.getElement().setAttribute("data-vote-target", "COMMENT:" + comment.getId());
        score.getStyle().set("font-weight", "600");

        Button upvote = new Button("▲");
        upvote.setAriaLabel("Upvote comment");
        upvote.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        Button downvote = new Button("▼");
        downvote.setAriaLabel("Downvote comment");
        downvote.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

        upvote.addClickListener(e -> castCommentVote(true, score, upvote, downvote));
        downvote.addClickListener(e -> castCommentVote(false, score, upvote, downvote));

        actions.add(upvote, score, downvote);

        Long currentUserId = getCurrentUserId();

        VoteValue existingVote = voteService.getCommentVote(currentUserId, comment.getId()).orElse(null);
        styleVoteSelection(upvote, downvote, existingVote);

        if (currentUserId != null && currentUserId.equals(comment.getAuthor().getId())) {
            Button delete = new Button("Delete", e -> deleteThisComment());
            delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            actions.add(delete);
        }

        return actions;
    }

    private void castCommentVote(boolean upvote, Span score, Button upvoteButton, Button downvoteButton) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Notification error = Notification.show("Please log in first.");
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        try {
            VoteValue requestedVote = upvote ? VoteValue.UPVOTE : VoteValue.DOWNVOTE;
            VoteResult result = voteService.toggleCommentVote(currentUserId, comment.getId(), requestedVote);
            score.setText(String.valueOf(result.score()));
            styleVoteSelection(upvoteButton, downvoteButton, result.currentVote());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            Notification error = Notification.show(exception.getMessage());
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Colors the upvote/downvote buttons based on which direction (if any) is
     * currently selected - same visual treatment as FeedView's post voting.
     */
    private void styleVoteSelection(Button upvote, Button downvote, VoteValue selection) {
        boolean upSelected = selection == VoteValue.UPVOTE;
        boolean downSelected = selection == VoteValue.DOWNVOTE;

        upvote.getElement().setAttribute("aria-pressed", String.valueOf(upSelected));
        downvote.getElement().setAttribute("aria-pressed", String.valueOf(downSelected));
        upvote.getStyle()
                .set("color", upSelected ? "#FF4500" : "#7c7c7c")
                .set("background", upSelected ? "#FFF1EB" : "transparent")
                .set("border-radius", "50%");
        downvote.getStyle()
                .set("color", downSelected ? "#7193FF" : "#7c7c7c")
                .set("background", downSelected ? "#EEF2FF" : "transparent")
                .set("border-radius", "50%");
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
            Long currentUserId = getCurrentUserId();

            if (currentUserId == null) {
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
                commentService.createComment(dto, currentUserId);
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
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return;
        }
        try {
            commentService.deleteComment(comment.getId(), currentUserId);
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
                    reply, depth + 1, commentService, userService, voteService, userSession,
                    onThreadChanged, replyFormCoordinator));
        }
    }
}