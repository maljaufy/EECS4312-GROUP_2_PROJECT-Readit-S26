package com.redditclone.shared.ui;

import com.redditclone.posts.ui.CreatePostView;
import com.redditclone.posts.ui.FeedView;
import com.redditclone.posts.ui.PopularView;
import com.redditclone.shared.security.UserSession;
import com.redditclone.subreddit.ui.AllSubredditsView;
import com.redditclone.subreddit.ui.CreateSubredditView;
import com.redditclone.user.ui.NotificationPreferencesView;
import com.redditclone.user.ui.ProfileView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;

/** Shared application shell so navigation remains visible across content pages. */
public class MainLayout extends AppLayout {

    public MainLayout(UserSession userSession) {
        setPrimarySection(Section.DRAWER);
        setDrawerOpened(true);
        addToNavbar(buildHeader());
        addToDrawer(buildSidebar(userSession));
        getElement().getStyle().set("background", "#DAE0E6");
    }

    private Component buildHeader() {
        DrawerToggle toggle = new DrawerToggle();
        H2 logo = new H2("Readit");
        logo.getStyle()
                .set("margin", "0")
                .set("color", "#FF4500")
                .set("font-size", "22px")
                .set("cursor", "pointer");
        logo.addClickListener(event -> getUI().ifPresent(ui -> ui.navigate("feed")));

        HorizontalLayout header = new HorizontalLayout(toggle, logo);
        header.setWidthFull();
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        header.getStyle()
                .set("background", "white")
                .set("border-bottom", "1px solid #d5d7da")
                .set("padding", "6px 12px");
        return header;
    }

    private Component buildSidebar(UserSession userSession) {
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
                link("Home", FeedView.class),
                link("Popular", PopularView.class),
                link("Recommendations", RecommendationsView.class),
                section("Create"),
                link("Create Post", CreatePostView.class),
                link("Start a Community", CreateSubredditView.class),
                link("All Communities", AllSubredditsView.class),
                section("Account"),
                link("Profile", ProfileView.class),
                link("Notification Settings", NotificationPreferencesView.class)
        );

        Button logout = new Button("Log out", event -> getUI().ifPresent(ui -> {
            userSession.clear(ui);
            ui.navigate("login");
        }));
        logout.setWidthFull();
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        logout.getStyle().set("margin-top", "auto");
        sidebar.add(logout);
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

    private RouterLink link(String label, Class<? extends Component> target) {
        RouterLink link = new RouterLink(label, target);
        link.getStyle()
                .set("width", "100%")
                .set("display", "block")
                .set("box-sizing", "border-box")
                .set("padding", "10px 12px")
                .set("border-radius", "8px")
                .set("color", "#1c1c1c")
                .set("text-decoration", "none");
        return link;
    }
}
