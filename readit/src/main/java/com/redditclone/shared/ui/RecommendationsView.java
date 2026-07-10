package com.redditclone.shared.ui;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("recommendations")
@PageTitle("Recommendations | Reddit Clone")
public class RecommendationsView extends Composite<VerticalLayout> {

    public RecommendationsView() {
        getContent().setSizeFull();
        getContent().setAlignItems(Alignment.CENTER);
        getContent().setJustifyContentMode(JustifyContentMode.CENTER);
        getContent().getStyle()
            .set("background", "linear-gradient(135deg, #556B2F 0%, #8B7355 100%)")
            .set("padding", "40px 20px");

        // Main container
        VerticalLayout mainContainer = new VerticalLayout();
        mainContainer.setMaxWidth("1000px");
        mainContainer.setWidthFull();
        mainContainer.setPadding(false);
        mainContainer.setSpacing(true);

        // Header with logout button
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.getStyle().set("margin-bottom", "20px");

        H2 logo = new H2("📱 Readit");
        logo.getStyle()
            .set("margin", "0")
            .set("color", "#F5DEB3");

        Button logoutButton = new Button("Logout", VaadinIcon.SIGN_OUT.create());
        logoutButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        logoutButton.getStyle()
            .set("background", "#D2B48C")
            .set("color", "#333")
            .set("font-weight", "600")
            .set("padding", "8px 16px");
        logoutButton.addClickListener(e -> handleLogout());

        headerLayout.add(logo, logoutButton);

        H1 title = new H1("Welcome to Readit!");
        title.getStyle()
            .set("margin", "0 0 10px 0")
            .set("text-align", "center")
            .set("font-size", "36px")
            .set("font-weight", "700")
            .set("color", "#F5DEB3");

        Paragraph subtitle = new Paragraph("Here are some popular communities to get you started");
        subtitle.getStyle()
            .set("margin", "0 0 40px 0")
            .set("text-align", "center")
            .set("color", "#666")
            .set("font-size", "18px");

        mainContainer.add(headerLayout, title, subtitle);

        // Subreddit recommendations
        H3 subredditHeader = new H3("🌟 Popular Subreddits");
        subredditHeader.getStyle()
            .set("margin", "0 0 20px 0")
            .set("color", "#F5DEB3");

        HorizontalLayout subredditCards = createSubredditCards();
        mainContainer.add(subredditHeader, subredditCards);

        // Action buttons
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        buttonLayout.setWidthFull();
        buttonLayout.setSpacing(true);
        buttonLayout.getStyle().set("margin-top", "40px");

        Button feedButton = new Button("Go to Feed", VaadinIcon.HOME.create());
        feedButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        feedButton.getStyle()
            .set("background", "#8B7355")
            .set("font-weight", "600")
            .set("padding", "12px 24px");
        feedButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("feed")));

        Button createPostButton = new Button("Create Post", VaadinIcon.PLUS.create());
        createPostButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        createPostButton.getStyle()
            .set("font-weight", "600")
            .set("padding", "12px 24px");
        createPostButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("create-post")));

        Button createSubredditButton = new Button("Create Subreddit", VaadinIcon.GROUP.create());
        createSubredditButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        createSubredditButton.getStyle()
            .set("font-weight", "600")
            .set("padding", "12px 24px");
        createSubredditButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("create-subreddit")));

        buttonLayout.add(feedButton, createPostButton, createSubredditButton);
        mainContainer.add(buttonLayout);

        getContent().add(mainContainer);
    }

    private HorizontalLayout createSubredditCards() {
        HorizontalLayout cards = new HorizontalLayout();
        cards.setWidthFull();
        cards.setJustifyContentMode(JustifyContentMode.CENTER);
        cards.setSpacing(true);
        cards.getStyle()
            .set("flex-wrap", "wrap")
            .set("gap", "20px");

        // Card 1
        VerticalLayout card1 = createSubredditCard("r/technology", "Latest tech news and discussions", "💻");
        // Card 2
        VerticalLayout card2 = createSubredditCard("r/gaming", "Gaming news, reviews, and community", "🎮");
        // Card 3
        VerticalLayout card3 = createSubredditCard("r/science", "Scientific discussion and research", "🔬");
        // Card 4
        VerticalLayout card4 = createSubredditCard("r/music", "Music discovery and discussion", "🎵");
        // Card 5
        VerticalLayout card5 = createSubredditCard("r/movies", "Film discussions and reviews", "🎬");
        // Card 6
        VerticalLayout card6 = createSubredditCard("r/food", "Recipes and food culture", "🍕");

        cards.add(card1, card2, card3, card4, card5, card6);
        return cards;
    }

    private VerticalLayout createSubredditCard(String name, String description, String emoji) {
        VerticalLayout card = new VerticalLayout();
        card.setWidth("220px");
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
            .set("background", "white")
            .set("border-radius", "12px")
            .set("box-shadow", "0 4px 16px rgba(0, 0, 0, 0.1)")
            .set("padding", "20px")
            .set("cursor", "pointer")
            .set("transition", "transform 0.2s, box-shadow 0.2s");

        card.addClassName("subreddit-card");

        H3 emojiTitle = new H3(emoji);
        emojiTitle.getStyle()
            .set("margin", "0 0 10px 0")
            .set("font-size", "32px")
            .set("text-align", "center");

        H3 nameTitle = new H3(name);
        nameTitle.getStyle()
            .set("margin", "0 0 8px 0")
            .set("font-size", "16px")
            .set("font-weight", "600")
            .set("color", "#8B7355")
            .set("text-align", "center");

        Paragraph desc = new Paragraph(description);
        desc.getStyle()
            .set("margin", "0")
            .set("color", "#666")
            .set("font-size", "13px")
            .set("text-align", "center");

        card.add(emojiTitle, nameTitle, desc);

        // Add hover effect
        card.getElement().addEventListener("mouseover", e -> {
            card.getStyle().set("transform", "translateY(-4px)");
            card.getStyle().set("box-shadow", "0 8px 24px rgba(0, 0, 0, 0.15)");
        });

        card.getElement().addEventListener("mouseout", e -> {
            card.getStyle().set("transform", "translateY(0)");
            card.getStyle().set("box-shadow", "0 4px 16px rgba(0, 0, 0, 0.1)");
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
