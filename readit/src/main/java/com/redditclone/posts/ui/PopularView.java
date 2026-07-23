package com.redditclone.posts.ui;

import com.redditclone.shared.ui.MainLayout;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "popular", layout = MainLayout.class)
@PageTitle("Popular | Reddit Clone")
public class PopularView extends VerticalLayout {

    public PopularView(PostService postService, SubredditService subredditService) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
            .set("background", "#DAE0E6")
            .set("margin", "0");

        // Header
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        header.getStyle()
            .set("background", "white")
            .set("padding", "16px 24px")
            .set("border-bottom", "1px solid #ccc");

        H2 title = new H2("🔥 Popular");
        title.getStyle()
            .set("margin", "0")
            .set("color", "#1c1c1c");

        Button logoutButton = new Button("Logout");
        logoutButton.getStyle()
            .set("background", "#FF4500")
            .set("color", "white")
            .set("font-weight", "600")
            .set("padding", "8px 16px")
            .set("border-radius", "20px");
        logoutButton.addClickListener(e -> handleLogout());

        header.add(title, logoutButton);

        // Content
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);
        content.setWidthFull();
        content.setMaxWidth("1200px");
        content.getStyle()
            .set("margin", "16px auto");

        // Get posts from last 24 hours and count by subreddit
        List<PostSummaryDto> recentPosts = postService.getPostsFromLast24Hours();
        Map<String, Long> subredditPostCounts = recentPosts.stream()
            .collect(Collectors.groupingBy(PostSummaryDto::subredditName, Collectors.counting()));

        // Sort by post count (descending)
        List<Map.Entry<String, Long>> sortedSubreddits = subredditPostCounts.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .toList();

        if (sortedSubreddits.isEmpty()) {
            content.add(new Paragraph("No posts in the last 24 hours. Be the first to create one!"));
        } else {
            for (Map.Entry<String, Long> entry : sortedSubreddits) {
                String subredditName = entry.getKey();
                Long postCount = entry.getValue();
                subredditService.findAll().stream()
                        .filter(s -> s.getName().equals(subredditName))
                        .findFirst().ifPresent(subreddit -> content.add(createSubredditCard(subreddit, postCount)));

            }
        }

        add(header, content);
    }

    private Div createSubredditCard(Subreddit subreddit, Long postCount) {
        Div card = new Div();
        card.getStyle()
            .set("background", "white")
            .set("border", "1px solid #ccc")
            .set("border-radius", "8px")
            .set("padding", "16px")
            .set("margin-bottom", "12px")
            .set("cursor", "pointer");

        H3 name = new H3("r/" + subreddit.getName());
        name.getStyle()
            .set("margin", "0 0 8px 0")
            .set("font-size", "20px")
            .set("font-weight", "600")
            .set("color", "#0079D3");

        Paragraph description = new Paragraph(
            subreddit.getDescription() != null ? subreddit.getDescription() : "No description"
        );
        description.getStyle()
            .set("margin", "0")
            .set("color", "#7c7c7c")
            .set("font-size", "14px");

        Span postCountSpan = new Span(postCount + " posts in the last 24 hours");
        postCountSpan.getStyle()
            .set("margin-top", "8px")
            .set("display", "block")
            .set("color", "#FF4500")
            .set("font-weight", "600")
            .set("font-size", "12px");

        card.add(name, description, postCountSpan);
        card.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("subreddit/" + subreddit.getId()));
        });

        // Hover effect
        card.getElement().addEventListener("mouseover", e -> {
            card.getStyle().set("border-color", "#0079D3");
        });

        card.getElement().addEventListener("mouseout", e -> {
            card.getStyle().set("border-color", "#ccc");
        });

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
