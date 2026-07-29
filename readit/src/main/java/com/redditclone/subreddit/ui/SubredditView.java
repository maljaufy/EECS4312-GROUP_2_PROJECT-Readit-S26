package com.redditclone.subreddit.ui;

import com.redditclone.shared.security.UserSession;
import com.redditclone.shared.ui.MainLayout;
import com.redditclone.posts.dto.PostSummaryDto;
import com.redditclone.posts.service.PostService;
import com.redditclone.subreddit.domain.Subreddit;
import com.redditclone.subreddit.service.SubredditService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "subreddit", layout = MainLayout.class)
@PageTitle("Subreddit | Reddit Clone")
public class SubredditView extends VerticalLayout implements HasUrlParameter<Long> {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm");

    private final PostService postService;
    private final SubredditService subredditService;
    private final UserSession userSession;
    private final VerticalLayout postList = new VerticalLayout();
    private final Button joinButton = new Button();

    private Subreddit currentSubreddit;
    private boolean isMember;
    private Long sessionUserId;

    public SubredditView(PostService postService, SubredditService subredditService,
                         UserSession userSession) {
        this.postService = postService;
        this.subredditService = subredditService;
        this.userSession = userSession;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("background", "#DAE0E6")
                .set("margin", "0");

        addAttachListener(event -> sessionUserId = userSession.currentUserId(event.getUI()));
    }

    @Override
    public void setParameter(BeforeEvent event, Long subredditId) {
        currentSubreddit = subredditService.getById(subredditId);
        // NOTE: assumes SubredditService exposes a membership check like this.
        // Adjust the method name/signature to whatever your service actually has.
        isMember = sessionUserId != null;
        buildView();
    }

    private void buildView() {
        removeAll();

        add(createBanner(), createContent());
        refreshPosts();
    }

    /** Reddit-style community banner: colored strip, icon, name, join button. */
    private Div createBanner() {
        Div banner = new Div();
        banner.getStyle()
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("background", "linear-gradient(135deg, #0079D3 0%, #0057A3 100%)")
                .set("padding", "0 0 20px 0");

        // Top color strip
        Div colorStrip = new Div();
        colorStrip.getStyle()
                .set("height", "80px")
                .set("width", "100%");
        banner.add(colorStrip);

        HorizontalLayout bannerContent = new HorizontalLayout();
        bannerContent.setWidthFull();
        bannerContent.setMaxWidth("1200px");
        bannerContent.getStyle()
                .set("margin", "0 auto")
                .set("padding", "0 24px")
                .set("box-sizing", "border-box")
                .set("align-items", "flex-end")
                .set("flex-wrap", "wrap")
                .set("gap", "16px");

        // Community icon
        Div icon = new Div(new Span("r/"));
        icon.getStyle()
                .set("width", "72px")
                .set("height", "72px")
                .set("border-radius", "50%")
                .set("background", "white")
                .set("border", "4px solid white")
                .set("box-shadow", "0 2px 6px rgba(0,0,0,0.2)")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("font-weight", "700")
                .set("font-size", "20px")
                .set("color", "#0079D3")
                .set("flex-shrink", "0");

        VerticalLayout titleBlock = new VerticalLayout();
        titleBlock.setSpacing(false);
        titleBlock.setPadding(false);
        titleBlock.getStyle().set("flex-grow", "1");

        H2 title = new H2("r/" + currentSubreddit.getName());
        title.getStyle()
                .set("margin", "0")
                .set("color", "white")
                .set("font-size", "26px")
                .set("font-weight", "700")
                .set("text-shadow", "0 1px 3px rgba(0,0,0,0.3)");

        Span subtitle = new Span("Community");
        subtitle.getStyle()
                .set("color", "rgba(255,255,255,0.85)")
                .set("font-size", "13px")
                .set("font-weight", "500");

        titleBlock.add(title, subtitle);

        HorizontalLayout actions = new HorizontalLayout();
        actions.setAlignItems(Alignment.CENTER);

        styleJoinButton();
        joinButton.addClickListener(e -> toggleMembership());

        actions.add(joinButton);

        bannerContent.add(icon, titleBlock, actions);
        bannerContent.setFlexGrow(1, titleBlock);

        banner.add(bannerContent);
        return banner;
    }

    /** Applies the visual state (label + colors) matching the current membership status. */
    private void styleJoinButton() {
        joinButton.setText(isMember ? "Joined" : "Join");
        joinButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_TERTIARY);

        if (isMember) {
            // "Joined" state: outlined/secondary look, like Reddit's actual toggle
            joinButton.getStyle()
                    .set("background", "transparent")
                    .set("color", "white")
                    .set("font-weight", "700")
                    .set("border", "1px solid white")
                    .set("border-radius", "20px")
                    .set("padding", "8px 20px");
        } else {
            joinButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            joinButton.getStyle()
                    .set("background", "white")
                    .set("color", "#0079D3")
                    .set("border", "none")
                    .set("font-weight", "700")
                    .set("border-radius", "20px")
                    .set("padding", "8px 20px");
        }
    }

    private void toggleMembership() {
        if (sessionUserId == null) {
            Notification.show("Please log in to join a community", 3_000, Notification.Position.MIDDLE);
            return;
        }

        try {
            // NOTE: assumes SubredditService exposes join/leave methods like these.
            // Adjust names/signatures to match your actual service.
            if (isMember) {
                isMember = false;
            } else {
                isMember = true;
            }
            styleJoinButton();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            Notification.show(exception.getMessage(), 3_000, Notification.Position.MIDDLE);
        }
    }

    private VerticalLayout createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);
        content.setWidthFull();
        content.setMaxWidth("1200px");
        content.getStyle()
                .set("margin", "0 auto")
                .set("padding-top", "20px");

        // Description card
        Div subredditInfo = new Div();
        subredditInfo.getStyle()
                .set("background", "white")
                .set("border", "1px solid #ccc")
                .set("border-radius", "8px")
                .set("padding", "20px 24px")
                .set("margin-bottom", "16px");

        H3 aboutHeader = new H3("About Community");
        aboutHeader.getStyle()
                .set("margin", "0 0 8px 0")
                .set("font-size", "14px")
                .set("font-weight", "700")
                .set("color", "#1c1c1c")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.5px");

        Paragraph description = new Paragraph(
                currentSubreddit.getDescription() != null && !currentSubreddit.getDescription().isBlank()
                        ? currentSubreddit.getDescription()
                        : "This community hasn't added a description yet."
        );
        description.getStyle()
                .set("margin", "0")
                .set("color", "#3c3c3c")
                .set("font-size", "14px")
                .set("line-height", "1.5");

        subredditInfo.add(aboutHeader, description);

        H3 postsHeader = new H3("Posts");
        postsHeader.getStyle()
                .set("margin", "0 0 12px 0")
                .set("font-size", "16px")
                .set("font-weight", "700")
                .set("color", "#1c1c1c");

        postList.setPadding(false);
        postList.setSpacing(true);
        postList.setWidthFull();

        content.add(subredditInfo, postsHeader, postList);
        return content;
    }

    private void refreshPosts() {
        postList.removeAll();
        List<PostSummaryDto> posts = postService.getFeed();

        List<PostSummaryDto> subredditPosts = posts.stream()
                .filter(post -> post.subredditName().equals(currentSubreddit.getName()))
                .toList();

        if (subredditPosts.isEmpty()) {
            postList.add(createEmptyState());
        } else {
            subredditPosts.forEach(post -> postList.add(card(post)));
        }
    }

    private Div createEmptyState() {
        Div emptyState = new Div();
        emptyState.getStyle()
                .set("text-align", "center")
                .set("padding", "48px 24px")
                .set("background", "white")
                .set("border", "1px solid #ccc")
                .set("border-radius", "8px")
                .set("width", "100%")
                .set("box-sizing", "border-box");

        H3 emptyIcon = new H3("\uD83D\uDCED");
        emptyIcon.getStyle().set("font-size", "40px").set("margin", "0 0 12px 0");

        H3 emptyTitle = new H3("No posts yet");
        emptyTitle.getStyle()
                .set("margin", "0 0 6px 0")
                .set("color", "#1c1c1c")
                .set("font-size", "16px")
                .set("font-weight", "700");

        Paragraph emptyText = new Paragraph("Be the first to post in r/" + currentSubreddit.getName() + "!");
        emptyText.getStyle()
                .set("margin", "0")
                .set("color", "#7c7c7c")
                .set("font-size", "13px");

        emptyState.add(emptyIcon, emptyTitle, emptyText);
        return emptyState;
    }

    private Div card(PostSummaryDto post) {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("box-sizing", "border-box")
                .set("background", "white")
                .set("border", "1px solid #ccc")
                .set("border-radius", "8px")
                .set("padding", "16px")
                .set("margin-bottom", "0")
                .set("cursor", "pointer")
                .set("transition", "border-color 0.15s ease");

        Span meta = new Span(
                "posted by u/" + post.authorUsername()
                        + "  \u2022  " + TIME.format(post.createdAt())
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
                .set("font-size", "14px")
                .set("line-height", "1.5");

        card.add(meta, title, body);

        card.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("post/" + post.id())));

        card.getElement().addEventListener("mouseover", e ->
                card.getStyle().set("border-color", "#0079D3"));
        card.getElement().addEventListener("mouseout", e ->
                card.getStyle().set("border-color", "#ccc"));

        return card;
    }
}