package com.redditclone.subreddit.ui;

import com.redditclone.posts.dto.PostSummaryDto;
import com.redditclone.posts.service.PostService;
import com.redditclone.subreddit.domain.Subreddit;
import com.redditclone.subreddit.service.SubredditService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route("subreddit")
@PageTitle("Subreddit | Reddit Clone")
public class SubredditView extends VerticalLayout implements HasUrlParameter<Long> {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm");

    private final PostService postService;
    private final SubredditService subredditService;
    private final VerticalLayout postList = new VerticalLayout();
    private Subreddit currentSubreddit;

    public SubredditView(PostService postService, SubredditService subredditService) {
        this.postService = postService;
        this.subredditService = subredditService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
            .set("background", "#DAE0E6")
            .set("margin", "0");
    }

    @Override
    public void setParameter(BeforeEvent event, Long subredditId) {
        currentSubreddit = subredditService.getById(subredditId);
        buildView();
    }

    private void buildView() {
        removeAll();

        // Header
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        header.getStyle()
            .set("background", "white")
            .set("padding", "16px 24px")
            .set("border-bottom", "1px solid #ccc");

        H2 title = new H2("r/" + currentSubreddit.getName());
        title.getStyle()
            .set("margin", "0")
            .set("color", "#0079D3");

        Button logoutButton = new Button("Logout");
        logoutButton.getStyle()
            .set("background", "#FF4500")
            .set("color", "white")
            .set("font-weight", "600")
            .set("padding", "8px 16px")
            .set("border-radius", "20px");
        logoutButton.addClickListener(e -> handleLogout());

        header.add(title, logoutButton);

        // Subreddit info
        Div subredditInfo = new Div();
        subredditInfo.getStyle()
            .set("background", "white")
            .set("padding", "24px")
            .set("margin-bottom", "16px");

        H3 subredditTitle = new H3("r/" + currentSubreddit.getName());
        subredditTitle.getStyle()
            .set("margin", "0 0 8px 0")
            .set("font-size", "28px")
            .set("font-weight", "600")
            .set("color", "#1c1c1c");

        Paragraph description = new Paragraph(
            currentSubreddit.getDescription() != null ? currentSubreddit.getDescription() : "No description"
        );
        description.getStyle()
            .set("margin", "0")
            .set("color", "#7c7c7c")
            .set("font-size", "14px");

        subredditInfo.add(subredditTitle, description);

        // Content
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);
        content.setWidthFull();
        content.setMaxWidth("1200px");
        content.getStyle()
            .set("margin", "0 auto");

        content.add(subredditInfo, postList);

        add(header, content);
        refreshPosts();
    }

    private void refreshPosts() {
        postList.removeAll();
        List<PostSummaryDto> posts = postService.getFeed();
        
        // Filter posts for this subreddit
        List<PostSummaryDto> subredditPosts = posts.stream()
            .filter(post -> post.subredditName().equals(currentSubreddit.getName()))
            .toList();

        if (subredditPosts.isEmpty()) {
            postList.add(new Paragraph("No posts in this subreddit yet. Be the first to create one!"));
        } else {
            subredditPosts.forEach(post -> postList.add(card(post)));
        }
    }

    private Div card(PostSummaryDto post) {
        Div card = new Div();
        card.getStyle()
            .set("background", "white")
            .set("border", "1px solid #ccc")
            .set("border-radius", "4px")
            .set("padding", "16px")
            .set("margin-bottom", "8px");

        Span meta = new Span(
            "posted by " + post.authorUsername()
            + "  •  " + TIME.format(post.createdAt())
        );
        meta.getStyle()
                .set("color", "#7c7c7c")
                .set("font-size", "12px")
                .set("font-weight", "600");

        H3 title = new H3(post.title());
        title.getStyle()
            .set("margin", "8px 0")
            .set("font-size", "18px")
            .set("font-weight", "600")
            .set("color", "#1c1c1c");

        Paragraph body = new Paragraph(post.content() == null ? "" : post.content());
        body.getStyle()
            .set("margin", "8px 0 0 0")
            .set("color", "#1c1c1c")
            .set("font-size", "14px");

        card.add(meta, title, body);
        return card;
    }

    private void handleLogout() {
        SecurityContextHolder.clearContext();
        getUI().ifPresent(ui -> {
            ui.getSession().setAttribute("jwt", null);
            ui.getSession().setAttribute("username", null);
            ui.navigate("");
        });
    }
}
