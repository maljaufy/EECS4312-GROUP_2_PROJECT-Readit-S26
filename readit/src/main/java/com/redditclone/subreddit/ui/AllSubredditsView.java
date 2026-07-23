package com.redditclone.subreddit.ui;

import com.redditclone.shared.ui.MainLayout;
import com.redditclone.subreddit.domain.Subreddit;
import com.redditclone.subreddit.service.SubredditService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Route(value = "all-subreddits", layout = MainLayout.class)
@PageTitle("All Subreddits | Reddit Clone")
public class AllSubredditsView extends VerticalLayout {

    public AllSubredditsView(SubredditService subredditService) {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
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

        H2 title = new H2("All Subreddits");
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
            .set("background", "white")
            .set("border-radius", "8px")
            .set("margin", "16px auto");

        List<Subreddit> subreddits = subredditService.findAll();
        
        if (subreddits.isEmpty()) {
            content.add(new Paragraph("No subreddits yet. Create one to get started!"));
        } else {
            for (Subreddit subreddit : subreddits) {
                content.add(createSubredditCard(subreddit));
            }
        }

        add(header, content);
    }

    private Div createSubredditCard(Subreddit subreddit) {
        Div card = new Div();
        card.getStyle()
            .set("background", "#F6F7F8")
            .set("border", "1px solid #ccc")
            .set("border-radius", "8px")
            .set("padding", "16px")
            .set("margin-bottom", "12px")
            .set("cursor", "pointer");

        H2 name = new H2("r/" + subreddit.getName());
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

        card.add(name, description);
        card.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("subreddit/" + subreddit.getId()));
        });

        // Hover effect
        card.getElement().addEventListener("mouseover", e -> {
            card.getStyle().set("background", "#E8E9EA");
        });

        card.getElement().addEventListener("mouseout", e -> {
            card.getStyle().set("background", "#F6F7F8");
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
