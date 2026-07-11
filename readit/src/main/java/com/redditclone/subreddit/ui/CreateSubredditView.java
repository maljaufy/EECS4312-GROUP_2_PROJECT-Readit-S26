package com.redditclone.subreddit.ui;

import com.redditclone.subreddit.service.SubredditService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("create-subreddit")
@PageTitle("Create Subreddit | Reddit Clone")
public class CreateSubredditView extends VerticalLayout {

    public CreateSubredditView(SubredditService subredditService) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle()
            .set("background", "linear-gradient(135deg, #556B2F 0%, #8B7355 100%)")
            .set("padding", "40px 20px");

        // Main container
        VerticalLayout mainContainer = new VerticalLayout();
        mainContainer.setMaxWidth("600px");
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

        H2 pageTitle = new H2("Create a subreddit");
        pageTitle.getStyle().set("color", "#F5DEB3");

        TextField name = new TextField("Subreddit name");
        name.setPlaceholder("e.g. programming");
        name.setRequiredIndicatorVisible(true);
        name.setMaxLength(50);
        name.setWidthFull();

        TextArea description = new TextArea("Description");
        description.setMaxLength(500);
        description.setWidthFull();

        Checkbox isPrivate = new Checkbox("Private community");

        Button create = new Button("Create subreddit", event -> {
            try {
                subredditService.create(name.getValue(), description.getValue(), isPrivate.getValue());
                Notification ok = Notification.show("Subreddit created.");
                ok.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                getUI().ifPresent(ui -> ui.navigate("feed"));
            } catch (IllegalArgumentException ex) {
                Notification error = Notification.show(ex.getMessage());
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        create.getStyle().set("background", "#8B7355");

        Button cancel = new Button("Cancel", e -> getUI().ifPresent(ui -> ui.navigate("feed")));

        mainContainer.add(headerLayout, pageTitle, name, description, isPrivate,
                new HorizontalLayout(create, cancel));
        add(mainContainer);
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
