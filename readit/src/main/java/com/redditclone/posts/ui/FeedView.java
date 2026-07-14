package com.redditclone.posts.ui;

import com.redditclone.posts.dto.PostSummaryDto;
import com.redditclone.posts.service.PostService;
import com.redditclone.subreddit.domain.Subreddit;
import com.redditclone.subreddit.service.SubredditService;
import com.redditclone.user.service.UserService;
import com.redditclone.voting.dto.VoteResult;
import com.redditclone.voting.service.VoteService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route("feed")
@PageTitle("Feed | Reddit Clone")
public class FeedView extends VerticalLayout {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm");

    private final PostService postService;
    private final SubredditService subredditService;
    private final VoteService voteService;
    private final UserService userService;
    private final VerticalLayout postList = new VerticalLayout();

    public FeedView(PostService postService, SubredditService subredditService,
                    VoteService voteService, UserService userService) {
        this.postService = postService;
        this.subredditService = subredditService;
        this.voteService = voteService;
        this.userService = userService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
            .set("background", "#DAE0E6")
            .set("margin", "0");

        // Top header bar
        HorizontalLayout topHeader = createTopHeader();
        
        // Three-column layout
        HorizontalLayout threeColumnLayout = new HorizontalLayout();
        threeColumnLayout.addClassName("feed-shell");
        threeColumnLayout.setWidthFull();
        threeColumnLayout.setHeightFull();
        threeColumnLayout.setSpacing(false);
        threeColumnLayout.setPadding(false);
        threeColumnLayout.getStyle().set("overflow", "hidden");

        // Left sidebar
        VerticalLayout leftSidebar = createLeftSidebar();
        leftSidebar.addClassName("feed-left-sidebar");
        leftSidebar.setWidth("270px");
        leftSidebar.setHeightFull();
        leftSidebar.getStyle()
            .set("position", "sticky")
            .set("top", "0")
            .set("flex-shrink", "0")
            .set("overflow-y", "auto")
            .set("padding", "16px 8px");

        // Center feed. The outer column consumes the available space while the
        // inner feed stays at a comfortable reading width.
        VerticalLayout centerFeed = createCenterFeed();
        VerticalLayout centerColumn = new VerticalLayout(centerFeed);
        centerColumn.addClassName("feed-center-column");
        centerColumn.setWidthFull();
        centerColumn.setHeightFull();
        centerColumn.setPadding(false);
        centerColumn.setSpacing(false);
        centerColumn.setAlignItems(Alignment.CENTER);
        centerColumn.getStyle()
            .set("padding", "20px 24px")
            .set("overflow-y", "auto");

        // Right sidebar
        VerticalLayout rightSidebar = createRightSidebar();
        rightSidebar.addClassName("feed-right-sidebar");
        rightSidebar.setWidth("312px");
        rightSidebar.setHeightFull();
        rightSidebar.getStyle()
            .set("position", "sticky")
            .set("top", "0")
            .set("flex-shrink", "0")
            .set("overflow-y", "auto")
            .set("padding", "16px 8px");

        threeColumnLayout.add(leftSidebar, centerColumn, rightSidebar);
        threeColumnLayout.expand(centerColumn);

        VerticalLayout mainLayout = new VerticalLayout(topHeader, threeColumnLayout);
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);

        add(mainLayout);
        addAttachListener(event -> registerVoteUpdateListener());
        refresh();
    }

    private HorizontalLayout createTopHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.addClassName("feed-header");
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        header.getStyle()
            .set("background", "#FFFFFF")
            .set("border-bottom", "1px solid #ccc")
            .set("padding", "8px 20px")
            .set("position", "sticky")
            .set("top", "0")
            .set("z-index", "1000");

        // Logo
        H2 logo = new H2("📱 Readit");
        logo.getStyle()
            .set("margin", "0")
            .set("color", "#FF4500")
            .set("font-size", "24px")
            .set("cursor", "pointer");
        logo.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("feed")));

        // Search bar
        TextField searchField = new TextField();
        searchField.setWidth("400px");
        searchField.setPlaceholder("Search Readit");
        searchField.setClearButtonVisible(true);
        searchField.getStyle()
            .set("background", "#F6F7F8")
            .set("border", "1px solid #ccc")
            .set("border-radius", "20px");
        
        searchField.addValueChangeListener(e -> {
            String query = e.getValue();
            if (query != null && !query.trim().isEmpty()) {
                performSearch(query.trim());
            } else {
                refresh();
            }
        });

        // Sort buttons (Hot, New, Top)
        HorizontalLayout sortButtons = new HorizontalLayout();
        sortButtons.setSpacing(true);
        sortButtons.setAlignItems(Alignment.CENTER);

        Button hotBtn = new Button("🔥 Hot");
        hotBtn.getStyle()
            .set("background", "#0079D3")
            .set("color", "white")
            .set("border", "none")
            .set("border-radius", "20px")
            .set("padding", "6px 16px")
            .set("font-weight", "700");

        Button newBtn = new Button("✨ New");
        newBtn.getStyle()
            .set("background", "transparent")
            .set("color", "#0079D3")
            .set("border", "none")
            .set("border-radius", "20px")
            .set("padding", "6px 16px")
            .set("font-weight", "700")
            .set("cursor", "pointer");

        Button topBtn = new Button("🏆 Top");
        topBtn.getStyle()
            .set("background", "transparent")
            .set("color", "#0079D3")
            .set("border", "none")
            .set("border-radius", "20px")
            .set("padding", "6px 16px")
            .set("font-weight", "700")
            .set("cursor", "pointer");

        sortButtons.add(hotBtn, newBtn, topBtn);

        // User actions
        HorizontalLayout userActions = new HorizontalLayout();
        userActions.setSpacing(true);
        userActions.setAlignItems(Alignment.CENTER);

        Button popularBtn = new Button("Popular");
        popularBtn.getStyle()
            .set("background", "transparent")
            .set("border", "none")
            .set("color", "#0079D3")
            .set("font-weight", "600")
            .set("cursor", "pointer");

        Button logoutButton = new Button("Logout", VaadinIcon.SIGN_OUT.create());
        logoutButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        logoutButton.getStyle()
            .set("background", "#FF4500")
            .set("color", "white")
            .set("font-weight", "600")
            .set("padding", "8px 16px")
            .set("border-radius", "20px");
        logoutButton.addClickListener(e -> handleLogout());

        userActions.add(sortButtons, popularBtn, logoutButton);

        header.add(logo, searchField, userActions);
        return header;
    }

    private VerticalLayout createLeftSidebar() {
        VerticalLayout sidebar = new VerticalLayout();
        sidebar.setSpacing(true);
        sidebar.setPadding(false);

        // Navigation links
        VerticalLayout navLinks = new VerticalLayout();
        navLinks.setSpacing(false);
        navLinks.setPadding(false);

        navLinks.add(createNavLink("🏠 Home", "feed"));
        navLinks.add(createNavLink("🔥 Popular", "feed"));
        navLinks.add(createNavLink("📰 News", "feed"));
        navLinks.add(createNavLink("🧭 Explore", "feed"));

        // Start a community button
        Button startCommunity = new Button("Start a community");
        startCommunity.getStyle()
            .set("width", "100%")
            .set("background", "#0079D3")
            .set("color", "white")
            .set("font-weight", "600")
            .set("padding", "12px")
            .set("border-radius", "20px")
            .set("margin-top", "16px");
        startCommunity.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("create-subreddit")));

        // Games on Reddit section
        VerticalLayout gamesSection = createSidebarSection("Games on Reddit", List.of("r/gaming", "r/Games", "r/GameDeals"));

        // Custom Feeds section
        VerticalLayout customFeedsSection = createSidebarSection("Custom Feeds", List.of("Home", "Popular"));

        // Communities section - dynamic
        VerticalLayout communitiesSection = createCommunitiesSection();

        sidebar.add(navLinks, startCommunity, gamesSection, customFeedsSection, communitiesSection);
        return sidebar;
    }

    private Div createNavLink(String text, String route) {
        Div link = new Div(text);
        link.getStyle()
            .set("padding", "10px 16px")
            .set("border-radius", "8px")
            .set("cursor", "pointer")
            .set("font-weight", "500")
            .set("color", "#1c1c1c")
            .set("display", "flex")
            .set("align-items", "center")
            .set("gap", "12px");
        link.addClassName("nav-link");
        link.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(route)));
        
        addHoverEffect(link);
        
        return link;
    }

    private VerticalLayout createSidebarSection(String title, List<String> items) {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(false);
        section.setPadding(false);
        section.getStyle()
            .set("margin-top", "24px")
            .set("background", "transparent");

        H4 header = new H4(title);
        header.getStyle()
            .set("margin", "0 0 8px 0")
            .set("font-size", "12px")
            .set("font-weight", "700")
            .set("color", "#7c7c7c")
            .set("text-transform", "uppercase")
            .set("padding", "0 16px");

        VerticalLayout itemsList = new VerticalLayout();
        itemsList.setSpacing(false);
        itemsList.setPadding(false);

        for (String item : items) {
            Div itemDiv = new Div(item);
            itemDiv.getStyle()
                .set("padding", "8px 16px")
                .set("border-radius", "8px")
                .set("cursor", "pointer")
                .set("font-weight", "500")
                .set("color", "#1c1c1c")
                .set("font-size", "14px");
            itemDiv.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("feed")));
            
            addHoverEffect(itemDiv);
            
            itemsList.add(itemDiv);
        }

        section.add(header, itemsList);
        return section;
    }

    private VerticalLayout createCenterFeed() {
        VerticalLayout feed = new VerticalLayout();
        feed.addClassName("feed-content");
        feed.setWidthFull();
        feed.setMaxWidth("900px");
        feed.setSpacing(true);
        feed.setPadding(false);

        // Create post input bar
        Div createPostBar = new Div();
        createPostBar.getStyle()
            .set("width", "100%")
            .set("box-sizing", "border-box")
            .set("background", "white")
            .set("border-radius", "4px")
            .set("padding", "12px")
            .set("border", "1px solid #ccc")
            .set("margin-bottom", "16px")
            .set("display", "flex")
            .set("align-items", "center")
            .set("gap", "12px");

        Span userAvatar = new Span("👤");
        userAvatar.getStyle()
            .set("font-size", "32px")
            .set("width", "38px")
            .set("height", "38px")
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "center");

        Span inputPlaceholder = new Span("Create Post");
        inputPlaceholder.getStyle()
            .set("background", "#F6F7F8")
            .set("border", "1px solid #ccc")
            .set("border-radius", "4px")
            .set("padding", "10px 16px")
            .set("flex-grow", "1")
            .set("cursor", "pointer")
            .set("color", "#7c7c7c");
        inputPlaceholder.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("create-post")));

        Button imageBtn = new Button("🖼️");
        imageBtn.getStyle()
            .set("background", "#F6F7F8")
            .set("border", "1px solid #ccc")
            .set("border-radius", "4px")
            .set("padding", "8px 12px")
            .set("cursor", "pointer");

        createPostBar.add(userAvatar, inputPlaceholder, imageBtn);

        // Feed title
        H3 feedTitle = new H3("Home");
        feedTitle.getStyle()
            .set("margin", "0 0 16px 0")
            .set("font-size", "18px")
            .set("font-weight", "600")
            .set("color", "#1c1c1c");

        postList.setPadding(false);
        postList.setWidthFull();
        postList.setSpacing(true);

        feed.add(createPostBar, feedTitle, postList);
        return feed;
    }

    private VerticalLayout createRightSidebar() {
        VerticalLayout sidebar = new VerticalLayout();
        sidebar.setSpacing(true);
        sidebar.setPadding(false);

        // Premium promotion
        Div premiumCard = new Div();
        premiumCard.getStyle()
            .set("background", "white")
            .set("border", "1px solid #ccc")
            .set("border-radius", "4px")
            .set("padding", "12px")
            .set("margin-bottom", "16px");

        H4 premiumTitle = new H4("Readit Premium");
        premiumTitle.getStyle()
            .set("margin", "0 0 8px 0")
            .set("font-size", "14px")
            .set("font-weight", "600")
            .set("color", "#1c1c1c");

        Paragraph premiumText = new Paragraph("The best Readit experience with monthly coins");
        premiumText.getStyle()
            .set("margin", "0 0 12px 0")
            .set("font-size", "12px")
            .set("color", "#7c7c7c");

        Button tryNow = new Button("Try Now");
        tryNow.getStyle()
            .set("background", "#FF4500")
            .set("color", "white")
            .set("border", "none")
            .set("border-radius", "20px")
            .set("padding", "8px 16px")
            .set("font-weight", "600")
            .set("width", "100%");

        premiumCard.add(premiumTitle, premiumText, tryNow);

        // Recent Posts section
        Div recentPostsCard = new Div();
        recentPostsCard.getStyle()
            .set("background", "white")
            .set("border", "1px solid #ccc")
            .set("border-radius", "4px")
            .set("padding", "12px");

        H4 recentTitle = new H4("Recent Posts");
        recentTitle.getStyle()
            .set("margin", "0 0 12px 0")
            .set("font-size", "14px")
            .set("font-weight", "600")
            .set("color", "#1c1c1c")
            .set("border-bottom", "1px solid #eee")
            .set("padding-bottom", "8px");

        VerticalLayout recentPostsList = new VerticalLayout();
        recentPostsList.setSpacing(false);
        recentPostsList.setPadding(false);

        List<PostSummaryDto> recentPosts = postService.getFeed().stream().limit(5).toList();
        if (recentPosts.isEmpty()) {
            Paragraph noRecentPosts = new Paragraph("New activity will appear here once posts are published.");
            noRecentPosts.getStyle()
                .set("color", "#667085")
                .set("font-size", "13px")
                .set("line-height", "1.5")
                .set("margin", "4px 0");
            recentPostsList.add(noRecentPosts);
        } else {
            recentPosts.forEach(post -> recentPostsList.add(createRecentPostItem(post)));
        }

        recentPostsCard.add(recentTitle, recentPostsList);

        sidebar.add(premiumCard, recentPostsCard);
        return sidebar;
    }

    private Div createRecentPostItem(PostSummaryDto post) {
        Div item = new Div();
        item.getStyle()
            .set("padding", "8px 0")
            .set("border-bottom", "1px solid #eee")
            .set("cursor", "pointer");

        Span subredditSpan = new Span("r/" + post.subredditName());
        subredditSpan.getStyle()
            .set("font-size", "12px")
            .set("font-weight", "600")
            .set("color", "#0079D3")
            .set("display", "block")
            .set("margin-bottom", "4px");

        Span titleSpan = new Span(post.title());
        titleSpan.getStyle()
            .set("font-size", "13px")
            .set("color", "#1c1c1c")
            .set("display", "block")
            .set("line-height", "1.4");

        item.add(subredditSpan, titleSpan);
        item.addClickListener(e -> getUI().ifPresent(
                ui -> ui.navigate("post/" + post.id() + "/comments")));
        
        addHoverEffect(item);
        
        return item;
    }

    private void addHoverEffect(Div element) {
        element.getElement().addEventListener("mouseover", e -> {
            element.getStyle().set("background", "#F6F7F8");
        });
        element.getElement().addEventListener("mouseout", e -> {
            element.getStyle().set("background", "transparent");
        });
    }

    private void refresh() {
        postList.removeAll();
        List<PostSummaryDto> feed = postService.getFeed();
        if (feed.isEmpty()) {
            Div emptyState = new Div();
            emptyState.getStyle()
                .set("text-align", "center")
                .set("padding", "52px 28px")
                .set("min-height", "260px")
                .set("box-sizing", "border-box")
                .set("background", "white")
                .set("border", "1px solid #ccc")
                .set("border-radius", "10px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("justify-content", "center");

            H3 emptyIcon = new H3("📭");
            emptyIcon.getStyle()
                .set("font-size", "48px")
                .set("margin", "0 0 16px 0");

            H3 emptyTitle = new H3("No posts yet");
            emptyTitle.getStyle()
                .set("margin", "0 0 8px 0")
                .set("color", "#1c1c1c")
                .set("font-size", "18px")
                .set("font-weight", "600");

            Paragraph emptyText = new Paragraph("Be the first to create a post and start the conversation!");
            emptyText.getStyle()
                .set("margin", "0 0 22px")
                .set("color", "#7c7c7c")
                .set("font-size", "14px");

            Button createPost = new Button("Create the first post",
                    event -> getUI().ifPresent(ui -> ui.navigate("create-post")));
            createPost.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            createPost.getStyle()
                .set("background", "#ff4500")
                .set("font-weight", "700");

            Button createCommunity = new Button("Start a community",
                    event -> getUI().ifPresent(ui -> ui.navigate("create-subreddit")));
            createCommunity.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            HorizontalLayout emptyActions = new HorizontalLayout(createPost, createCommunity);
            emptyActions.setJustifyContentMode(JustifyContentMode.CENTER);
            emptyActions.getStyle().set("flex-wrap", "wrap");

            emptyState.add(emptyIcon, emptyTitle, emptyText, emptyActions);
            postList.add(emptyState);
            return;
        }
        feed.forEach(post -> postList.add(card(post)));
    }

    private Component card(PostSummaryDto post) {
        // Main card container
        Div card = new Div();
        card.getStyle()
                .set("background", "white")
                .set("border", "1px solid #ccc")
                .set("border-radius", "4px")
                .set("margin-bottom", "0")
                .set("cursor", "pointer")
                .set("display", "flex")
                .set("flex-direction", "column");

        // Vote section on the left
        Div voteSection = new Div();
        voteSection.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("padding", "8px")
                .set("background", "#F8F9FA")
                .set("border-right", "1px solid #eee")
                .set("min-width", "40px");

        Button upvoteBtn = new Button("▲");
        upvoteBtn.getStyle()
                .set("background", "transparent")
                .set("border", "none")
                .set("color", "#7c7c7c")
                .set("font-size", "16px")
                .set("padding", "4px")
                .set("cursor", "pointer");

        Span voteCount = new Span(String.valueOf(voteService.getPostScore(post.id())));
        voteCount.getElement().setAttribute("data-vote-target", "POST:" + post.id());
        voteCount.getStyle()
                .set("font-weight", "700")
                .set("font-size", "12px")
                .set("color", "#1c1c1c")
                .set("margin", "4px 0");

        Button downvoteBtn = new Button("▼");
        downvoteBtn.getStyle()
                .set("background", "transparent")
                .set("border", "none")
                .set("color", "#7c7c7c")
                .set("font-size", "16px")
                .set("padding", "4px")
                .set("cursor", "pointer");

        upvoteBtn.addClickListener(event -> castPostVote(post.id(), true, voteCount));
        downvoteBtn.addClickListener(event -> castPostVote(post.id(), false, voteCount));

        voteSection.add(upvoteBtn, voteCount, downvoteBtn);

        // Content section
        Div contentSection = new Div();
        contentSection.getStyle()
                .set("flex-grow", "1")
                .set("padding", "12px")
                .set("display", "flex")
                .set("flex-direction", "column");

        // Post metadata
        Span meta = new Span("r/" + post.subredditName()
                + "  \u2022  posted by u/" + post.authorUsername()
                + "  \u2022  " + TIME.format(post.createdAt()));
        meta.getStyle()
                .set("color", "#7c7c7c")
                .set("font-size", "12px")
                .set("font-weight", "500")
                .set("margin-bottom", "8px");

        // Post title
        H3 title = new H3(post.title());
        title.getStyle()
                .set("margin", "0 0 8px 0")
                .set("font-size", "18px")
                .set("font-weight", "600")
                .set("color", "#1c1c1c")
                .set("line-height", "1.4");

        // Post body
        Paragraph body = new Paragraph(post.content() == null ? "" : post.content());
        body.getStyle()
                .set("margin", "0 0 12px 0")
                .set("color", "#1c1c1c")
                .set("font-size", "14px")
                .set("line-height", "1.5");

        // Action buttons (comments, share, save)
        HorizontalLayout actionButtons = new HorizontalLayout();
        actionButtons.setSpacing(true);
        actionButtons.getStyle()
                .set("margin-top", "8px")
                .set("padding-top", "8px")
                .set("border-top", "1px solid #eee");

        Button commentsBtn = new Button("💬 Comments");
        commentsBtn.getStyle()
                .set("background", "transparent")
                .set("border", "none")
                .set("color", "#7c7c7c")
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("padding", "4px 8px")
                .set("cursor", "pointer");
        commentsBtn.addClickListener(event ->
                getUI().ifPresent(ui -> ui.navigate("post/" + post.id() + "/comments")));

        Button shareBtn = new Button("🔗 Share");
        shareBtn.getStyle()
                .set("background", "transparent")
                .set("border", "none")
                .set("color", "#7c7c7c")
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("padding", "4px 8px")
                .set("cursor", "pointer");

        Button saveBtn = new Button("🔖 Save");
        saveBtn.getStyle()
                .set("background", "transparent")
                .set("border", "none")
                .set("color", "#7c7c7c")
                .set("font-size", "12px")
                .set("font-weight", "600")
                .set("padding", "4px 8px")
                .set("cursor", "pointer");

        actionButtons.add(commentsBtn, shareBtn, saveBtn);

        contentSection.add(meta, title, body, actionButtons);

        // Combine vote and content sections
        HorizontalLayout cardLayout = new HorizontalLayout(voteSection, contentSection);
        cardLayout.setSpacing(false);
        cardLayout.setWidthFull();
        cardLayout.getStyle().set("align-items", "flex-start");

        card.add(cardLayout);
        card.addClassName("post-card");

        // Hover effect
        card.getElement().addEventListener("mouseover", e -> {
            card.getStyle().set("border-color", "#0079D3");
        });

        card.getElement().addEventListener("mouseout", e -> {
            card.getStyle().set("border-color", "#ccc");
        });

        return card;
    }

    private void castPostVote(Long postId, boolean upvote, Span voteCount) {
        try {
            Long voterId = currentUserId();
            VoteResult result = upvote
                    ? voteService.upvotePost(voterId, postId)
                    : voteService.downvotePost(voterId, postId);
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

    private void handleLogout() {
        SecurityContextHolder.clearContext();
        getUI().ifPresent(ui -> {
            ui.getSession().setAttribute("jwt", null);
            ui.getSession().setAttribute("username", null);
            ui.getSession().setAttribute("userId", null);
            ui.navigate("");
        });
    }

    private VerticalLayout createCommunitiesSection() {
        VerticalLayout section = new VerticalLayout();
        section.setSpacing(false);
        section.setPadding(false);
        section.getStyle()
            .set("margin-top", "24px")
            .set("background", "transparent");

        H4 header = new H4("Communities");
        header.getStyle()
            .set("margin", "0 0 8px 0")
            .set("font-size", "12px")
            .set("font-weight", "700")
            .set("color", "#7c7c7c")
            .set("text-transform", "uppercase")
            .set("padding", "0 16px");

        VerticalLayout itemsList = new VerticalLayout();
        itemsList.setSpacing(false);
        itemsList.setPadding(false);

        List<Subreddit> subreddits = subredditService.findAll();
        for (Subreddit subreddit : subreddits) {
            Div itemDiv = new Div("r/" + subreddit.getName());
            itemDiv.getStyle()
                .set("padding", "8px 16px")
                .set("border-radius", "8px")
                .set("cursor", "pointer")
                .set("font-weight", "500")
                .set("color", "#1c1c1c")
                .set("font-size", "14px");
            itemDiv.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("feed")));
            
            addHoverEffect(itemDiv);
            
            itemsList.add(itemDiv);
        }

        section.add(header, itemsList);
        return section;
    }

    private void performSearch(String query) {
        postList.removeAll();
        
        // Search posts
        List<PostSummaryDto> postResults = postService.searchPosts(query);
        
        // Search subreddits
        List<Subreddit> subredditResults = subredditService.searchSubreddits(query);
        
        if (postResults.isEmpty() && subredditResults.isEmpty()) {
            Div emptyState = new Div();
            emptyState.getStyle()
                .set("text-align", "center")
                .set("padding", "40px 20px")
                .set("background", "white")
                .set("border", "1px solid #ccc")
                .set("border-radius", "4px");

            H3 emptyIcon = new H3("🔍");
            emptyIcon.getStyle()
                .set("font-size", "48px")
                .set("margin", "0 0 16px 0");

            H3 emptyTitle = new H3("No results found");
            emptyTitle.getStyle()
                .set("margin", "0 0 8px 0")
                .set("color", "#1c1c1c")
                .set("font-size", "18px")
                .set("font-weight", "600");

            Paragraph emptyText = new Paragraph("Try searching for something else");
            emptyText.getStyle()
                .set("margin", "0")
                .set("color", "#7c7c7c")
                .set("font-size", "14px");

            emptyState.add(emptyIcon, emptyTitle, emptyText);
            postList.add(emptyState);
        } else {
            // Display subreddit results first
            if (!subredditResults.isEmpty()) {
                H4 subredditHeader = new H4("Communities");
                subredditHeader.getStyle()
                    .set("margin", "0 0 12px 0")
                    .set("font-size", "14px")
                    .set("font-weight", "700")
                    .set("color", "#1c1c1c");
                postList.add(subredditHeader);
                
                for (Subreddit subreddit : subredditResults) {
                    Div subredditCard = createSubredditCard(subreddit);
                    postList.add(subredditCard);
                }
            }
            
            // Display post results
            if (!postResults.isEmpty()) {
                H4 postsHeader = new H4("Posts");
                postsHeader.getStyle()
                    .set("margin", "24px 0 12px 0")
                    .set("font-size", "14px")
                    .set("font-weight", "700")
                    .set("color", "#1c1c1c");
                postList.add(postsHeader);
                
                postResults.forEach(post -> postList.add(card(post)));
            }
        }
    }
    
    private Div createSubredditCard(Subreddit subreddit) {
        Div card = new Div();
        card.getStyle()
            .set("background", "white")
            .set("border", "1px solid #ccc")
            .set("border-radius", "4px")
            .set("padding", "16px")
            .set("margin-bottom", "8px")
            .set("cursor", "pointer")
            .set("display", "flex")
            .set("align-items", "center")
            .set("gap", "12px");

        Span icon = new Span("📁");
        icon.getStyle()
            .set("font-size", "32px")
            .set("width", "40px")
            .set("height", "40px")
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "center");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);

        Span name = new Span("r/" + subreddit.getName());
        name.getStyle()
            .set("font-weight", "600")
            .set("font-size", "16px")
            .set("color", "#0079D3");

        Span description = new Span(subreddit.getDescription() != null ? subreddit.getDescription() : "No description");
        description.getStyle()
            .set("font-size", "14px")
            .set("color", "#7c7c7c")
            .set("margin-top", "4px");

        content.add(name, description);
        card.add(icon, content);
        
        card.addClickListener(e -> {
            // Navigate to subreddit-specific feed (for now, just refresh feed)
            // TODO: Create subreddit-specific view
            getUI().ifPresent(ui -> ui.navigate("feed"));
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
}
