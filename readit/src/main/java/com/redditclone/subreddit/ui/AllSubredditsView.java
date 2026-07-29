package com.redditclone.subreddit.ui;

import com.redditclone.shared.ui.MainLayout;
import com.redditclone.subreddit.domain.Subreddit;
import com.redditclone.subreddit.service.SubredditService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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

@Route(value = "all-subreddits", layout = MainLayout.class)
@PageTitle("All Subreddits | Reddit Clone")
public class AllSubredditsView extends VerticalLayout {

    public AllSubredditsView(SubredditService subredditService) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("background", "#DAE0E6")
                .set("margin", "0");

        add(createHeader(), createContent(subredditService));
    }

    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        header.getStyle()
                .set("background", "white")
                .set("padding", "16px 24px")
                .set("border-bottom", "1px solid #ccc")
                .set("box-sizing", "border-box");

        VerticalLayout titleBlock = new VerticalLayout();
        titleBlock.setSpacing(false);
        titleBlock.setPadding(false);

        H2 title = new H2("All Communities");
        title.getStyle()
                .set("margin", "0")
                .set("color", "#1c1c1c")
                .set("font-size", "22px");

        Span subtitle = new Span("Discover and join communities across Readit");
        subtitle.getStyle()
                .set("color", "#7c7c7c")
                .set("font-size", "13px");

        titleBlock.add(title, subtitle);

        Button logoutButton = new Button("Logout");
        logoutButton.getStyle()
                .set("background", "#FF4500")
                .set("color", "white")
                .set("font-weight", "600")
                .set("padding", "8px 16px")
                .set("border-radius", "20px");
        logoutButton.addClickListener(e -> handleLogout());

        header.add(titleBlock, logoutButton);
        return header;
    }

    private VerticalLayout createContent(SubredditService subredditService) {
        VerticalLayout outer = new VerticalLayout();
        outer.setWidthFull();
        outer.setPadding(true);
        outer.setSpacing(false);
        outer.setAlignItems(Alignment.CENTER);

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);
        content.setWidthFull();
        content.setMaxWidth("900px");

        List<Subreddit> subreddits = subredditService.findAll();

        if (subreddits.isEmpty()) {
            content.add(createEmptyState());
        } else {
            Span countLabel = new Span(subreddits.size() + " communit" + (subreddits.size() == 1 ? "y" : "ies"));
            countLabel.getStyle()
                    .set("color", "#7c7c7c")
                    .set("font-size", "13px")
                    .set("font-weight", "600")
                    .set("margin-bottom", "12px")
                    .set("display", "block");
            content.add(countLabel);

            for (Subreddit subreddit : subreddits) {
                content.add(createSubredditCard(subreddit));
            }
        }

        outer.add(content);
        return outer;
    }

    private Div createEmptyState() {
        Div emptyState = new Div();
        emptyState.getStyle()
                .set("text-align", "center")
                .set("padding", "60px 28px")
                .set("background", "white")
                .set("border", "1px solid #ccc")
                .set("border-radius", "10px")
                .set("width", "100%")
                .set("box-sizing", "border-box");

        H3 icon = new H3("\uD83C\uDFD8\uFE0F");
        icon.getStyle().set("font-size", "48px").set("margin", "0 0 16px 0");

        H3 emptyTitle = new H3("No communities yet");
        emptyTitle.getStyle()
                .set("margin", "0 0 8px 0")
                .set("color", "#1c1c1c")
                .set("font-size", "18px")
                .set("font-weight", "700");

        Paragraph emptyText = new Paragraph("Be the first to start a community on Readit!");
        emptyText.getStyle()
                .set("margin", "0 0 20px")
                .set("color", "#7c7c7c")
                .set("font-size", "14px");

        Button createCommunity = new Button("Start a community",
                e -> getUI().ifPresent(ui -> ui.navigate("create-subreddit")));
        createCommunity.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createCommunity.getStyle()
                .set("background", "#0079D3")
                .set("font-weight", "700")
                .set("border-radius", "20px");

        emptyState.add(icon, emptyTitle, emptyText, createCommunity);
        return emptyState;
    }

    private Div createSubredditCard(Subreddit subreddit) {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("box-sizing", "border-box")
                .set("background", "white")
                .set("border", "1px solid #ccc")
                .set("border-radius", "8px")
                .set("padding", "16px")
                .set("margin-bottom", "10px")
                .set("cursor", "pointer")
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "14px")
                .set("transition", "border-color 0.15s ease, box-shadow 0.15s ease");

        Div icon = new Div(new Span(subreddit.getName().isEmpty() ? "r" :
                subreddit.getName().substring(0, 1).toUpperCase()));
        icon.getStyle()
                .set("width", "44px")
                .set("height", "44px")
                .set("min-width", "44px")
                .set("border-radius", "50%")
                .set("background", "#0079D3")
                .set("color", "white")
                .set("font-weight", "700")
                .set("font-size", "18px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center");

        VerticalLayout textBlock = new VerticalLayout();
        textBlock.setSpacing(false);
        textBlock.setPadding(false);
        textBlock.getStyle().set("flex-grow", "1").set("min-width", "0");

        Span name = new Span("r/" + subreddit.getName());
        name.getStyle()
                .set("font-weight", "700")
                .set("font-size", "15px")
                .set("color", "#1c1c1c");

        Paragraph description = new Paragraph(
                subreddit.getDescription() != null && !subreddit.getDescription().isBlank()
                        ? subreddit.getDescription()
                        : "No description"
        );
        description.getStyle()
                .set("margin", "2px 0 0 0")
                .set("color", "#7c7c7c")
                .set("font-size", "13px")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis")
                .set("white-space", "nowrap");

        textBlock.add(name, description);

        Button viewButton = new Button("View");
        viewButton.getStyle()
                .set("background", "transparent")
                .set("color", "#0079D3")
                .set("border", "1px solid #0079D3")
                .set("border-radius", "16px")
                .set("font-weight", "700")
                .set("font-size", "12px")
                .set("padding", "4px 14px")
                .set("flex-shrink", "0");

        card.add(icon, textBlock, viewButton);
        card.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("subreddit/" + subreddit.getId())));

        card.getElement().addEventListener("mouseover", e -> {
            card.getStyle()
                    .set("border-color", "#0079D3")
                    .set("box-shadow", "0 2px 8px rgba(0,0,0,0.08)");
        });
        card.getElement().addEventListener("mouseout", e -> {
            card.getStyle()
                    .set("border-color", "#ccc")
                    .set("box-shadow", "none");
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