package com.redditclone.posts.ui;

import com.redditclone.posts.dto.PostSummaryDto;
import com.redditclone.posts.service.PostService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
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
    private final VerticalLayout postList = new VerticalLayout();

    public FeedView(PostService postService) {
        this.postService = postService;

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

        Div card = new Div(content);
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "8px")
                .set("padding", "var(--lumo-space-m)")
                .set("width", "100%")
                .set("box-sizing", "border-box");
        return card;
    }
}
