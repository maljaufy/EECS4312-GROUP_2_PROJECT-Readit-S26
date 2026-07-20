package com.redditclone.comments.ui;

import com.redditclone.comments.domain.Comment;
import com.redditclone.comments.dto.CommentDto;
import com.redditclone.comments.service.CommentService;
import com.redditclone.user.service.UserService;
import com.redditclone.voting.domain.VoteValue;
import com.redditclone.voting.dto.VoteResult;
import com.redditclone.voting.service.VoteService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
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
 *
 * NOTE: identity is read from the Vaadin session ("userId" attribute, set at
 * login) rather than userService.getCurrentUser() / SecurityContextHolder.
 * This is a workaround while the session-vs-JWT security config is unresolved -
 * see the "userId" attribute set in LoginView.
 */
public class CommentThreadComponent extends VerticalLayout {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");
    private static final int INDENT_PER_LEVEL_PX = 20;
    private static final int MAX_INDENT_PX = 280; // cap indentation so deep threads don't run off-screen

    private static final String REDDIT_ORANGE = "#FF4500";
    private static final String REDDIT_BLUE = "#7193FF";
    private static final String MUTED_GRAY = "#878A8C";
    private static final String INK = "#1c1c1c";

    private final Comment comment;
    private final CommentService commentService;
    private final UserService userService;
    private final VoteService voteService;
    private final int depth;
    private final Runnable onThreadChanged;
    private final ReplyFormCoordinator replyFormCoordinator;

    private VerticalLayout repliesContainer;
    private VerticalLayout replyFormElement;
    private boolean showingReplyForm = false;
    private VoteValue currentVote;
    private Button upvoteBtn;
    private Button downvoteBtn;

    public CommentThreadComponent(Comment comment,
                                  int depth,
                                  CommentService commentService,
                                  UserService userService,
                                  VoteService voteService,
                                  Runnable onThreadChanged,
                                  ReplyFormCoordinator replyFormCoordinator) {
        this.comment = comment;
        this.depth = depth;
        this.commentService = commentService;
        this.userService = userService;
        this.voteService = voteService;
        this.onThreadChanged = onThreadChanged;
        this.replyFormCoordinator = replyFormCoordinator;

        setPadding(false);
        setSpacing(false);
        setWidthFull();

        int indent = Math.min(depth * INDENT_PER_LEVEL_PX, MAX_INDENT_PX);
        getStyle().set("margin-left", indent + "px");
        getStyle().set("padding-left", depth > 0 ? "14px" : "0px");
        getStyle().set("border-left", depth > 0 ? "2px solid #edeff1" : "none");
        getStyle().set("margin-top", depth > 0 ? "10px" : "16px");
        getStyle().set("width", "calc(100% - " + indent + "px)");
        getStyle().set("transition", "border-color 0.15s ease");

        // Subtle hover highlight on the thread guide line, like reddit.com
        getElement().addEventListener("mouseenter", e -> {
            if (depth > 0) {
                getStyle().set("border-left", "2px solid " + MUTED_GRAY);
            }
        }).addEventData("event.stopPropagation");
        getElement().addEventListener("mouseleave", e -> {
            if (depth > 0) {
                getStyle().set("border-left", "2px solid #edeff1");
            }
        }).addEventData("event.stopPropagation");

        add(buildHeader());
        add(buildBody());
        add(buildActionRow());

        repliesContainer = new VerticalLayout();
        repliesContainer.setPadding(false);
        repliesContainer.setSpacing(false);
        repliesContainer.setWidthFull();
        add(repliesContainer);

        loadReplies();

        // getUI()/session isn't available until the component is attached,
        // so the initial vote lookup has to happen here, not in the constructor body.
        addAttachListener(event -> {
            currentVote = voteService.getCurrentCommentVote(getCurrentUserId(), comment.getId());
            styleVoteButtons(upvoteBtn, downvoteBtn);
        });
    }

    /**
     * Reads the logged-in user's ID from the Vaadin session, or null if nobody's logged in.
     * Session attribute is set once, at login, in LoginView.
     */
    private Long getCurrentUserId() {
        return (Long) getUI()
                .map(ui -> ui.getSession().getAttribute("userId"))
                .orElse(null);
    }

    private HorizontalLayout buildHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setSpacing(false);
        header.setPadding(false);
        header.getStyle().set("align-items", "center").set("gap", "6px");

        Span avatar = new Span("●");
        avatar.getStyle()
                .set("color", avatarColorFor(comment.getAuthor().getUsername()))
                .set("font-size", "18px")
                .set("line-height", "1")
                .set("margin-right", "2px");

        Span author = new Span("u/" + comment.getAuthor().getUsername());
        author.getStyle()
                .set("font-weight", "700")
                .set("font-size", "12.5px")
                .set("color", INK);

        Span dot = new Span("•");
        dot.getStyle().set("color", MUTED_GRAY).set("font-size", "11px");

        Span timestamp = new Span(comment.getCreatedAt().format(TIMESTAMP_FORMAT));
        timestamp.getStyle()
                .set("color", MUTED_GRAY)
                .set("font-size", "12px");

        header.add(avatar, author, dot, timestamp);
        return header;
    }

    private Div buildBody() {
        Div body = new Div(new Span(comment.getBody()));
        body.getStyle()
                .set("white-space", "pre-wrap")
                .set("color", INK)
                .set("font-size", "14px")
                .set("line-height", "1.4")
                .set("margin", "2px 0 4px 0");
        return body;
    }

    private HorizontalLayout buildActionRow() {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(false);
        actions.setPadding(false);
        actions.getStyle().set("align-items", "center").set("gap", "2px");

        upvoteBtn = new Button("▲");
        upvoteBtn.setAriaLabel("Upvote comment");
        upvoteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        styleArrowButton(upvoteBtn);

        Span score = new Span(String.valueOf(voteService.getCommentScore(comment.getId())));
        score.getElement().setAttribute("data-vote-target", "COMMENT:" + comment.getId());
        score.getStyle()
                .set("font-weight", "700")
                .set("font-size", "12px")
                .set("color", MUTED_GRAY)
                .set("min-width", "18px")
                .set("text-align", "center");

        downvoteBtn = new Button("▼");
        downvoteBtn.setAriaLabel("Downvote comment");
        downvoteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        styleArrowButton(downvoteBtn);

        upvoteBtn.addClickListener(e -> castCommentVote(VoteValue.UPVOTE, score, upvoteBtn, downvoteBtn));
        downvoteBtn.addClickListener(e -> castCommentVote(VoteValue.DOWNVOTE, score, upvoteBtn, downvoteBtn));

        Button reply = new Button("Reply", e -> toggleReplyForm());
        styleTextActionButton(reply);
        reply.getStyle().set("margin-left", "10px");

        actions.add(upvoteBtn, score, downvoteBtn, reply);

        Long currentUserId = getCurrentUserId();

        if (currentUserId != null && currentUserId.equals(comment.getAuthor().getId())) {
            Button delete = new Button("Delete", e -> deleteThisComment());
            styleTextActionButton(delete);
            delete.getStyle().set("color", "#d93a00");
            actions.add(delete);
        }

        return actions;
    }

    private void styleArrowButton(Button button) {
        button.getStyle()
                .set("background", "transparent")
                .set("border", "none")
                .set("font-size", "13px")
                .set("padding", "2px 6px")
                .set("min-width", "0")
                .set("cursor", "pointer");
    }

    private void styleTextActionButton(Button button) {
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        button.getStyle()
                .set("background", "transparent")
                .set("border", "none")
                .set("color", MUTED_GRAY)
                .set("font-size", "12px")
                .set("font-weight", "700")
                .set("padding", "2px 6px")
                .set("min-width", "0")
                .set("cursor", "pointer");
    }

    private String avatarColorFor(String username) {
        // Deterministic pseudo-random color per user, like reddit's avatar tinting
        String[] palette = {"#0079D3", "#FF4500", "#46D160", "#FFB000", "#7193FF", "#E4462F"};
        int index = Math.abs(username.hashCode()) % palette.length;
        return palette[index];
    }

    private void castCommentVote(VoteValue clickedValue, Span score, Button upvoteBtn, Button downvoteBtn) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Notification error = Notification.show("Please log in first.");
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        try {
            VoteResult result;
            if (currentVote == clickedValue) {
                result = voteService.removeCommentVote(currentUserId, comment.getId());
                currentVote = null;
            } else {
                result = clickedValue == VoteValue.UPVOTE
                        ? voteService.upvoteComment(currentUserId, comment.getId())
                        : voteService.downvoteComment(currentUserId, comment.getId());
                currentVote = clickedValue;
            }
            score.setText(String.valueOf(result.score()));
            styleVoteButtons(upvoteBtn, downvoteBtn);
            score.getStyle().set("color", currentVote == null ? MUTED_GRAY
                    : currentVote == VoteValue.UPVOTE ? REDDIT_ORANGE : REDDIT_BLUE);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            Notification error = Notification.show(exception.getMessage());
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void styleVoteButtons(Button upvoteBtn, Button downvoteBtn) {
        if (upvoteBtn == null || downvoteBtn == null) {
            return;
        }
        upvoteBtn.getStyle().set("color", currentVote == VoteValue.UPVOTE ? REDDIT_ORANGE : MUTED_GRAY);
        downvoteBtn.getStyle().set("color", currentVote == VoteValue.DOWNVOTE ? REDDIT_BLUE : MUTED_GRAY);
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
        form.getStyle().set("margin-top", "6px");

        TextArea replyText = new TextArea();
        replyText.setPlaceholder("Write a reply...");
        replyText.setWidthFull();
        replyText.setMinHeight("80px");
        replyText.getStyle().set("--vaadin-input-field-border-radius", "8px");

        Button submit = new Button("Reply", e -> {
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
        submit.getStyle()
                .set("background", REDDIT_ORANGE)
                .set("border-radius", "20px")
                .set("font-weight", "700");

        Button cancel = new Button("Cancel", e -> toggleReplyForm());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        cancel.getStyle().set("border-radius", "20px");

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
                    reply, depth + 1, commentService, userService, voteService,
                    onThreadChanged, replyFormCoordinator));
        }
    }
}