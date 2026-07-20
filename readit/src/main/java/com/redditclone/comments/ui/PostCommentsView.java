package com.redditclone.comments.ui;

import com.redditclone.comments.domain.Comment;
import com.redditclone.comments.dto.CommentDto;
import com.redditclone.comments.service.CommentService;
import com.redditclone.posts.domain.Post;
import com.redditclone.posts.service.PostService;
import com.redditclone.user.service.UserService;
import com.redditclone.voting.domain.VoteValue;
import com.redditclone.voting.dto.VoteResult;
import com.redditclone.voting.service.VoteService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * NOTE: identity is read from the Vaadin session ("userId" attribute, set at
 * login) rather than userService.getCurrentUser() / SecurityContextHolder.
 * This is a workaround while the session-vs-JWT security config is unresolved -
 * see the "userId" attribute set in LoginView.
 */
@Route("post/:postId/comments")
@PageTitle("Comments | Reddit Clone")
public class PostCommentsView extends VerticalLayout implements BeforeEnterObserver {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm");

    private static final String REDDIT_ORANGE = "#FF4500";
    private static final String REDDIT_BLUE = "#7193FF";
    private static final String MUTED_GRAY = "#878A8C";
    private static final String INK = "#1c1c1c";

    private final PostService postService;
    private final CommentService commentService;
    private final UserService userService;
    private final VoteService voteService;

    // One coordinator per page load - shared by every CommentThreadComponent in this
    // thread so that opening a reply box anywhere closes whichever one was open before.
    private final ReplyFormCoordinator replyFormCoordinator = new ReplyFormCoordinator();

    private VerticalLayout commentsContainer;
    private Post post;
    private VoteValue currentPostVote;
    private Button postUpvoteBtn;
    private Button postDownvoteBtn;
    private Span postScoreLabel;

    public PostCommentsView(PostService postService,
                            CommentService commentService,
                            UserService userService,
                            VoteService voteService) {
        this.postService = postService;
        this.commentService = commentService;
        this.userService = userService;
        this.voteService = voteService;

        setPadding(true);
        setSpacing(false);
        setMaxWidth("800px");
        getStyle().set("margin", "0 auto");
        addAttachListener(event -> registerVoteUpdateListener());
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

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<String> postIdParam = event.getRouteParameters().get("postId");

        if (postIdParam.isEmpty()) {
            removeAll();
            add(new H2("Post not found"));
            return;
        }

        Long postId;
        try {
            postId = Long.parseLong(postIdParam.get());
        } catch (NumberFormatException invalidId) {
            removeAll();
            add(new H2("Post not found"));
            return;
        }

        try {
            this.post = postService.getPostById(postId);
        } catch (IllegalArgumentException notFound) {
            removeAll();
            add(new H2("Post not found"));
            return;
        }

        this.currentPostVote = voteService.getCurrentPostVote(getCurrentUserId(), post.getId());

        removeAll();
        add(buildTopBar());
        add(new H2(post.getTitle()));

        if (post.getContent() != null && !post.getContent().isBlank()) {
            add(new Paragraph(post.getContent()));
        }

        add(buildPostVoteRow());

        add(buildNewCommentForm());
        add(divider());

        add(new H3("Comments"));

        commentsContainer = new VerticalLayout();
        commentsContainer.setPadding(false);
        commentsContainer.setSpacing(false);
        commentsContainer.setWidthFull();
        add(commentsContainer);

        loadTopLevelComments();
    }

    /**
     * Top row: "r/subreddit • posted by u/author • date at time" with a button
     * back to the main feed.
     */
    private VerticalLayout buildTopBar() {
        VerticalLayout bar = new VerticalLayout();
        bar.setWidthFull();
        Span metaLine = new Span("r/" + post.getSubreddit().getName()
                + "  •  posted by u/" + post.getAuthor().getUsername()
                + "  •  " + post.getCreatedAt().format(TIMESTAMP_FORMAT));
        metaLine.getStyle()
                .set("color", MUTED_GRAY)
                .set("font-size", "13px")
                .set("font-weight", "500")
        .set("margin-bottom","6px");

        Button backToFeed = new Button("Back to feed", VaadinIcon.ARROW_LEFT.create());
        backToFeed.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        backToFeed.getStyle()
                .set("color", "#0079D3")
                .set("font-weight", "600");
        backToFeed.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("feed")));

        bar.add(backToFeed,metaLine);
        return bar;
    }

    /**
     * Upvote/downvote row for the post itself, styled to match the comment-level
     * vote arrows in CommentThreadComponent (same colors, same toggle behavior).
     */
    private HorizontalLayout buildPostVoteRow() {
        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(false);
        row.setPadding(false);
        row.getStyle().set("align-items", "center").set("gap", "4px").set("margin", "8px 0");

        postUpvoteBtn = new Button("▲");
        postUpvoteBtn.setAriaLabel("Upvote post");
        postUpvoteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        styleArrowButton(postUpvoteBtn);

        postScoreLabel = new Span(String.valueOf(voteService.getPostScore(post.getId())));
        postScoreLabel.getElement().setAttribute("data-vote-target", "POST:" + post.getId());
        postScoreLabel.getStyle()
                .set("font-weight", "700")
                .set("font-size", "13px")
                .set("min-width", "20px")
                .set("text-align", "center");

        postDownvoteBtn = new Button("▼");
        postDownvoteBtn.setAriaLabel("Downvote post");
        postDownvoteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        styleArrowButton(postDownvoteBtn);

        postUpvoteBtn.addClickListener(e -> castPostVote(VoteValue.UPVOTE));
        postDownvoteBtn.addClickListener(e -> castPostVote(VoteValue.DOWNVOTE));

        styleVoteButtons();

        row.add(postUpvoteBtn, postScoreLabel, postDownvoteBtn);
        return row;
    }

    private void castPostVote(VoteValue clickedValue) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            Notification error = Notification.show("Please log in first.");
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        try {
            VoteResult result;
            if (currentPostVote == clickedValue) {
                result = voteService.removePostVote(currentUserId, post.getId());
                currentPostVote = null;
            } else {
                result = clickedValue == VoteValue.UPVOTE
                        ? voteService.upvotePost(currentUserId, post.getId())
                        : voteService.downvotePost(currentUserId, post.getId());
                currentPostVote = clickedValue;
            }
            postScoreLabel.setText(String.valueOf(result.score()));
            styleVoteButtons();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            Notification error = Notification.show(exception.getMessage());
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void styleVoteButtons() {
        postUpvoteBtn.getStyle().set("color", currentPostVote == VoteValue.UPVOTE ? REDDIT_ORANGE : MUTED_GRAY);
        postDownvoteBtn.getStyle().set("color", currentPostVote == VoteValue.DOWNVOTE ? REDDIT_BLUE : MUTED_GRAY);
        postScoreLabel.getStyle().set("color", currentPostVote == null ? MUTED_GRAY
                : currentPostVote == VoteValue.UPVOTE ? REDDIT_ORANGE : REDDIT_BLUE);
    }

    private void styleArrowButton(Button button) {
        button.getStyle()
                .set("background", "transparent")
                .set("border", "none")
                .set("font-size", "15px")
                .set("padding", "2px 8px")
                .set("min-width", "0")
                .set("cursor", "pointer");
    }

    private Hr divider() {
        Hr hr = new Hr();
        hr.getStyle()
                .set("border", "none")
                .set("border-top", "1px solid #edeff1")
                .set("margin", "12px 0");
        return hr;
    }

    private VerticalLayout buildNewCommentForm() {
        VerticalLayout form = new VerticalLayout();
        form.setPadding(false);
        form.setSpacing(true);

        TextArea body = new TextArea("Add a comment");
        body.setWidthFull();
        body.setMinHeight("100px");

        Button submit = new Button("Post comment", e -> {
            Long currentUserId = getCurrentUserId();

            if (currentUserId == null) {
                Notification error = Notification.show("Please log in first.");
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                getUI().ifPresent(ui -> ui.navigate("login"));
                return;
            }
            if (body.getValue() == null || body.getValue().isBlank()) {
                Notification error = Notification.show("Comment can't be empty.");
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                CommentDto dto = new CommentDto(post.getId(), null, body.getValue());
                commentService.createComment(dto, currentUserId);
                body.clear();
                loadTopLevelComments();
                Notification ok = Notification.show("Comment posted.");
                ok.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalArgumentException ex) {
                Notification error = Notification.show(ex.getMessage());
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.getStyle()
                .set("background", REDDIT_ORANGE)
                .set("border-radius", "20px")
                .set("font-weight", "700");

        form.add(body, new HorizontalLayout(submit));
        return form;
    }

    private void loadTopLevelComments() {
        commentsContainer.removeAll();
        List<Comment> topLevel = commentService.findTopLevelByPost(post);

        if (topLevel.isEmpty()) {
            commentsContainer.add(new Paragraph("No comments yet. Be the first to reply."));
            return;
        }

        for (Comment comment : topLevel) {
            commentsContainer.add(new CommentThreadComponent(
                    comment, 0, commentService, userService, voteService,
                    this::loadTopLevelComments, replyFormCoordinator));
            commentsContainer.add(divider());
        }
    }

    private void registerVoteUpdateListener() {
        getUI().ifPresent(ui -> ui.getPage().executeJs("""
                if (!window.readitVoteUpdateListenerRegistered) {
                  window.readitVoteUpdateListenerRegistered = true;
                  window.addEventListener('readit-vote-updated', function(event) {
                    const detail = event.detail;
                    const target = detail.targetType + ':' + detail.targetId;
                    document.querySelectorAll('[data-vote-target]').forEach(function(element) {
                      if (element.getAttribute('data-vote-target') === target) {
                        element.textContent = String(detail.score);
                      }
                    });
                  });
                }
                """));
    }
}