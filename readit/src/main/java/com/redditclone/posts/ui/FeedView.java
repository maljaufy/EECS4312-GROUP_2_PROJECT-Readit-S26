package com.redditclone.posts.ui;

import com.redditclone.posts.dto.PostSummaryDto;
import com.redditclone.posts.service.PostService;
import com.redditclone.user.service.UserService;
import com.redditclone.voting.dto.VoteResult;
import com.redditclone.voting.service.VoteService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route("feed")
@PageTitle("Feed | Reddit Clone")
public class FeedView extends VerticalLayout {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm");

    private final PostService postService;
    private final VoteService voteService;
    private final UserService userService;
    private final VerticalLayout postList = new VerticalLayout();

    public FeedView(PostService postService, VoteService voteService, UserService userService) {
        this.postService = postService;
        this.voteService = voteService;
        this.userService = userService;

        setPadding(true);
        setSpacing(true);
        setMaxWidth("800px");

        H1 header = new H1("Readit");
        Button createPost = new Button("Create Post",
                e -> getUI().ifPresent(ui -> ui.navigate("create-post")));
        Button createSubreddit = new Button("Create Subreddit",
                e -> getUI().ifPresent(ui -> ui.navigate("create-subreddit")));
        HorizontalLayout toolbar = new HorizontalLayout(createPost, createSubreddit);

        postList.setPadding(false);
        postList.setWidthFull();

        add(header, toolbar, new H3("Latest posts"), postList);
        addAttachListener(event -> registerVoteUpdateListener());
        refresh();
    }

    private void refresh() {
        postList.removeAll();
        List<PostSummaryDto> feed = postService.getFeed();
        if (feed.isEmpty()) {
            postList.add(new Paragraph("No posts yet. Be the first to create one."));
            return;
        }
        feed.forEach(post -> postList.add(card(post)));
    }

    private Component card(PostSummaryDto post) {
        Span meta = new Span("r/" + post.subredditName()
                + "  \u2022  posted by " + post.authorUsername()
                + "  \u2022  " + TIME.format(post.createdAt()));
        meta.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        Button title = new Button(post.title(), e ->
                getUI().ifPresent(ui -> ui.navigate("post/" + post.id() + "/comments")));
        title.getStyle().set("margin", "0.25em 0");

        Paragraph body = new Paragraph(post.content() == null ? "" : post.content());
        VerticalLayout content = new VerticalLayout(meta, title, body);
        content.setPadding(false);
        content.setSpacing(false);
        content.setWidthFull();

        Span voteCount = new Span(String.valueOf(voteService.getPostScore(post.id())));
        voteCount.getElement().setAttribute("data-vote-target", "POST:" + post.id());
        Button upvote = new Button("▲", e -> castPostVote(post.id(), true, voteCount));
        Button downvote = new Button("▼", e -> castPostVote(post.id(), false, voteCount));
        VerticalLayout votes = new VerticalLayout(upvote, voteCount, downvote);
        votes.setPadding(false);
        votes.setSpacing(false);
        votes.setAlignItems(Alignment.CENTER);

        HorizontalLayout layout = new HorizontalLayout(votes, content);
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.expand(content);

        Div card = new Div(layout);
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "8px")
                .set("padding", "var(--lumo-space-m)")
                .set("width", "100%")
                .set("box-sizing", "border-box");
        return card;
    }

    private void castPostVote(Long postId, boolean upvote, Span voteCount) {
        try {
            VoteResult result = upvote
                    ? voteService.upvotePost(currentUserId(), postId)
                    : voteService.downvotePost(currentUserId(), postId);
            voteCount.setText(String.valueOf(result.score()));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            Notification.show(exception.getMessage(), 3_000, Notification.Position.MIDDLE);
        }
    }

    private Long currentUserId() {
        Long sessionUserId = getUI()
                .map(ui -> (Long) ui.getSession().getAttribute("userId"))
                .orElse(null);
        return sessionUserId == null ? userService.getCurrentUser().getId() : sessionUserId;
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
