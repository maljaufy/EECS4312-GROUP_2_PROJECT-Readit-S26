package com.redditclone.shared.ui;

import com.redditclone.posts.ui.CreatePostView;
import com.redditclone.posts.ui.FeedView;
import com.redditclone.posts.ui.PopularView;
import com.redditclone.shared.security.UserSession;
import com.redditclone.subreddit.ui.AllSubredditsView;
import com.redditclone.subreddit.ui.CreateSubredditView;
import com.redditclone.user.ui.NotificationPreferencesView;
import com.redditclone.user.ui.ProfileView;
import com.redditclone.user.ui.LoginView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

/** Shared application shell so navigation remains visible across content pages. */
public class MainLayout extends AppLayout implements BeforeEnterObserver {

    public static final String PENDING_FEED_SEARCH = "pendingFeedSearch";
    private final UserSession userSession;

    public MainLayout(UserSession userSession) {
        this.userSession = userSession;
        // The navbar is primary so it spans the entire viewport, with the
        // drawer beginning below it like Reddit's desktop layout.
        setPrimarySection(Section.NAVBAR);
        setDrawerOpened(true);
        addToNavbar(buildHeader(userSession));
        addToDrawer(buildSidebar());
        getElement().getStyle().set("background", "#DAE0E6");
    }

    /**
     * All content views use this layout. Keep unauthenticated browser requests
     * inside the Vaadin router so they reach the login page rather than a 403.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (userSession.currentUserId(event.getUI()) == null) {
            event.rerouteTo(LoginView.class);
        }
    }

    private Component buildHeader(UserSession userSession) {
        DrawerToggle toggle = new DrawerToggle();
        H2 logo = new H2("Readit");
        logo.getStyle()
                .set("margin", "0")
                .set("color", "#FF4500")
                .set("font-size", "22px")
                .set("cursor", "pointer");
        logo.addClickListener(event -> getUI().ifPresent(ui -> ui.navigate("feed")));

        TextField search = new TextField();
        search.setPlaceholder("Search Readit");
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setMaxWidth("640px");
        search.setWidthFull();
        search.getStyle()
                .set("background", "#F6F7F8")
                .set("border-radius", "24px");
        search.addValueChangeListener(event -> {
            String query = event.getValue() == null ? "" : event.getValue().trim();
            if (getContent() instanceof FeedView feedView) {
                feedView.search(query);
            } else if (!query.isEmpty()) {
                getUI().ifPresent(ui -> {
                    ui.getSession().setAttribute(PENDING_FEED_SEARCH, query);
                    ui.navigate(FeedView.class);
                });
            }
        });

        RouterLink create = new RouterLink("Create", CreatePostView.class);
        create.getStyle()
                .set("font-weight", "700")
                .set("color", "#1c1c1c")
                .set("text-decoration", "none")
                .set("white-space", "nowrap");

        RouterLink profile = new RouterLink("Profile", ProfileView.class);
        profile.getStyle()
                .set("font-weight", "600")
                .set("color", "#1c1c1c")
                .set("text-decoration", "none")
                .set("white-space", "nowrap");

        Button logout = new Button("Log out", event -> getUI().ifPresent(ui -> {
            userSession.clear(ui);
            ui.navigate("login");
        }));
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

        HorizontalLayout actions = new HorizontalLayout(create, profile, logout);
        actions.setAlignItems(HorizontalLayout.Alignment.CENTER);
        actions.setSpacing(true);
        actions.setWidth("280px");
        actions.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        HorizontalLayout brand = new HorizontalLayout(logo, toggle);
        brand.setAlignItems(HorizontalLayout.Alignment.CENTER);
        brand.setWidth("280px");
        brand.setFlexShrink(0, logo, toggle);

        HorizontalLayout searchRegion = new HorizontalLayout(search);
        searchRegion.setWidthFull();
        searchRegion.setJustifyContentMode(HorizontalLayout.JustifyContentMode.CENTER);
        searchRegion.setAlignItems(HorizontalLayout.Alignment.CENTER);

        HorizontalLayout header = new HorizontalLayout(brand, searchRegion, actions);
        header.setWidthFull();
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        header.expand(searchRegion);
        header.getStyle()
                .set("background", "white")
                .set("border-bottom", "1px solid #d5d7da")
                .set("box-sizing", "border-box")
                .set("height", "64px")
                .set("padding", "8px 20px")
                .set("gap", "12px");
        return header;
    }

    private Component buildSidebar() {
        VerticalLayout sidebar = new VerticalLayout();
        sidebar.setPadding(true);
        sidebar.setSpacing(false);
        sidebar.setWidth("260px");
        sidebar.setHeightFull();
        sidebar.getStyle()
                .set("background", "#FFFFFF")
                .set("border-right", "1px solid #d5d7da")
                .set("overflow-y", "auto");

        sidebar.add(
                section("Browse"),
                link("Home", VaadinIcon.HOME, FeedView.class),
                link("Popular", VaadinIcon.TRENDING_UP, PopularView.class),
                link("Recommendations", VaadinIcon.LIGHTBULB, RecommendationsView.class),
                section("Create"),
                link("Create Post", VaadinIcon.EDIT, CreatePostView.class),
                link("Start a Community", VaadinIcon.PLUS_CIRCLE, CreateSubredditView.class),
                link("All Communities", VaadinIcon.GROUP, AllSubredditsView.class),
                section("Account"),
                link("Profile", VaadinIcon.USER, ProfileView.class),
                link("Notification Settings", VaadinIcon.BELL, NotificationPreferencesView.class)
        );

        return sidebar;
    }

    private Span section(String label) {
        Span section = new Span(label);
        section.getStyle()
                .set("font-size", "11px")
                .set("font-weight", "700")
                .set("text-transform", "uppercase")
                .set("color", "#667085")
                .set("padding", "18px 12px 6px");
        return section;
    }

    private RouterLink link(String label, VaadinIcon iconType,
                            Class<? extends Component> target) {
        RouterLink link = new RouterLink("", target);
        Icon icon = iconType.create();
        icon.setSize("20px");
        Span text = new Span(label);
        link.add(icon, text);
        link.getStyle()
                .set("width", "100%")
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "14px")
                .set("box-sizing", "border-box")
                .set("padding", "10px 12px")
                .set("border-radius", "8px")
                .set("color", "#1c1c1c")
                .set("text-decoration", "none");
        return link;
    }
}
