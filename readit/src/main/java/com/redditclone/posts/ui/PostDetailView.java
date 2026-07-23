package com.redditclone.posts.ui;

import com.redditclone.comments.service.CommentService;
import com.redditclone.comments.ui.PostCommentsView;
import com.redditclone.posts.domain.Post;
import com.redditclone.posts.service.PostService;
import com.redditclone.shared.security.UserSession;
import com.redditclone.shared.ui.MainLayout;
import com.redditclone.user.service.UserService;
import com.redditclone.voting.dto.VoteResult;
import com.redditclone.voting.domain.VoteValue;
import com.redditclone.voting.service.VoteService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Route(value = "post/:postId", layout = MainLayout.class)
@PageTitle("Post | Reddit Clone")
public class PostDetailView extends VerticalLayout implements BeforeEnterObserver {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm");

    private final PostService postService;
    private final VoteService voteService;
    private final UserSession userSession;
    private final PostCommentsView commentsView;

    private Post post;
    private Long sessionUserId;

    public PostDetailView(PostService postService, VoteService voteService,
                          CommentService commentService, UserService userService,
                          UserSession userSession) {
        this.postService = postService;
        this.voteService = voteService;
        this.userSession = userSession;
        this.commentsView = new PostCommentsView(
                commentService, userService, voteService);

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);
        getStyle().set("background", "#DAE0E6");
        addAttachListener(event -> registerVoteUpdateListener());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        sessionUserId = userSession.currentUserId(event.getUI());

        Optional<String> postIdParam = event.getRouteParameters().get("postId");
        if (postIdParam.isEmpty()) {
            showNotFound();
            return;
        }

        long postId;
        try {
            postId = Long.parseLong(postIdParam.get());
        } catch (NumberFormatException invalidId) {
            showNotFound();
            return;
        }

        try {
            this.post = postService.getPostById(postId);
        } catch (IllegalArgumentException notFound) {
            showNotFound();
            return;
        }

        commentsView.showForPost(post);
        add(buildBackButton(), buildCard(), commentsView);
    }

    private Button buildBackButton() {
        Button back = new Button("← Back to feed",
                event -> getUI().ifPresent(ui -> ui.navigate("feed")));
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        back.getStyle()
                .set("align-self", "flex-start")
                .set("font-weight", "600");
        return back;
    }

    private Div buildCard() {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("max-width", "800px")
                .set("background", "white")
                .set("border", "1px solid #ccc")
                .set("border-radius", "4px")
                .set("box-sizing", "border-box");

        HorizontalLayout layout = new HorizontalLayout(buildVoteColumn(), buildContent());
        layout.setSpacing(false);
        layout.setWidthFull();
        layout.getStyle().set("align-items", "flex-start");

        card.add(layout);
        return card;
    }

    private Div buildVoteColumn() {
        Div voteSection = new Div();
        voteSection.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("padding", "8px")
                .set("background", "#F8F9FA")
                .set("border-right", "1px solid #eee")
                .set("min-width", "48px");

        Button upvote = voteButton("\u25B2");
        Button downvote = voteButton("\u25BC");

        Span score = new Span(String.valueOf(voteService.getPostScore(post.getId())));
        score.getElement().setAttribute("data-vote-target", "POST:" + post.getId());
        score.getStyle()
                .set("font-weight", "700")
                .set("font-size", "13px")
                .set("color", "#1c1c1c")
                .set("margin", "4px 0");

        VoteValue existingVote = voteService.getPostVote(currentUserId(), post.getId())
                .orElse(null);
        styleVoteSelection(upvote, downvote, existingVote);

        upvote.addClickListener(event -> castVote(true, score, upvote, downvote));
        downvote.addClickListener(event -> castVote(false, score, upvote, downvote));

        voteSection.add(upvote, score, downvote);
        return voteSection;
    }

    private Button voteButton(String label) {
        Button button = new Button(label);
        button.getStyle()
                .set("background", "transparent")
                .set("border", "none")
                .set("color", "#7c7c7c")
                .set("font-size", "16px")
                .set("padding", "4px")
                .set("cursor", "pointer");
        return button;
    }

    private VerticalLayout buildContent() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(false);
        content.setWidthFull();

        Span meta = new Span("r/" + post.getSubreddit().getName()
                + "  \u2022  posted by u/" + post.getAuthor().getUsername()
                + "  \u2022  " + TIME.format(post.getCreatedAt()));
        meta.getStyle()
                .set("color", "#7c7c7c")
                .set("font-size", "12px")
                .set("font-weight", "500");

        H2 title = new H2(post.getTitle());
        title.getStyle()
                .set("margin", "8px 0")
                .set("font-size", "22px")
                .set("color", "#1c1c1c");

        Paragraph body = new Paragraph(post.getContent() == null ? "" : post.getContent());
        body.getStyle()
                .set("color", "#1c1c1c")
                .set("font-size", "15px")
                .set("line-height", "1.6")
                .set("white-space", "pre-wrap");

        content.add(meta, title, body, buildActions());
        return content;
    }

    private HorizontalLayout buildActions() {
        Button comments = actionButton("\uD83D\uDCAC Comments");
        comments.addClickListener(event -> commentsView.getElement().executeJs(
                "this.scrollIntoView({ behavior: 'smooth', block: 'start' })"));

        Button backToFeed = actionButton("\u2190 Back to feed");
        backToFeed.addClickListener(event -> getUI().ifPresent(ui -> ui.navigate("feed")));

        HorizontalLayout actions = new HorizontalLayout(comments, backToFeed);
        actions.setSpacing(true);
        actions.getStyle()
                .set("margin-top", "12px")
                .set("padding-top", "8px")
                .set("border-top", "1px solid #eee")
                .set("flex-wrap", "wrap");

        if (postService.isAuthor(post.getId(), currentUserId())) {
            Button edit = actionButton("\u270E Edit");
            edit.addClickListener(event ->
                    getUI().ifPresent(ui -> ui.navigate("post/" + post.getId() + "/edit")));

            Button delete = actionButton("\uD83D\uDDD1 Delete");
            delete.getStyle().set("color", "#d93025");
            delete.addClickListener(event -> confirmDelete());

            actions.add(edit, delete);
        }

        return actions;
    }

    private Button actionButton(String label) {
        Button button = new Button(label);
        button.getStyle()
                .set("background", "transparent")
                .set("border", "none")
                .set("color", "#7c7c7c")
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("padding", "4px 8px")
                .set("cursor", "pointer");
        return button;
    }

    private void confirmDelete() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Delete this post?");

        Paragraph message = new Paragraph(
                "This removes the post and all of its comments. It cannot be undone.");

        Button cancel = new Button("Cancel", event -> dialog.close());

        Button confirm = new Button("Delete", event -> {
            dialog.close();
            deletePost();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        dialog.add(message);
        dialog.getFooter().add(cancel, confirm);
        dialog.open();
    }

    private void deletePost() {
        try {
            postService.deletePost(post.getId(), currentUserId());
            Notification ok = Notification.show("Post deleted.");
            ok.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            getUI().ifPresent(ui -> ui.navigate("feed"));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            Notification error = Notification.show(failure.getMessage());
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void castVote(boolean upvote, Span score,
                          Button upvoteButton, Button downvoteButton) {
        Long voterId = currentUserId();
        if (voterId == null) {
            Notification error = Notification.show("Please log in first.");
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            getUI().ifPresent(ui -> ui.navigate("login"));
            return;
        }
        try {
            VoteValue requestedVote = upvote ? VoteValue.UPVOTE : VoteValue.DOWNVOTE;
            VoteResult result = voteService.togglePostVote(
                    voterId, post.getId(), requestedVote);
            score.setText(String.valueOf(result.score()));
            styleVoteSelection(upvoteButton, downvoteButton, result.currentVote());
        } catch (IllegalArgumentException | IllegalStateException failure) {
            Notification.show(failure.getMessage(), 3_000, Notification.Position.MIDDLE);
        }
    }

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

    private Long currentUserId() {
        if (sessionUserId != null) {
            return sessionUserId;
        }
        return getUI().map(userSession::currentUserId).orElse(null);
    }

    private void showNotFound() {
        add(new H2("Post not found"));
        Button backToFeed = new Button("Back to feed",
                event -> getUI().ifPresent(ui -> ui.navigate("feed")));
        backToFeed.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(backToFeed);
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
