package com.redditclone.subreddit.ui;

import com.redditclone.subreddit.service.SubredditService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
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
        setJustifyContentMode(JustifyContentMode.START);
        addClassName("creation-view");
        getStyle()
            .set("background", "#f3f4f6")
            .set("overflow-y", "auto")
            .set("padding", "32px 20px");

        // Main container
        VerticalLayout mainContainer = new VerticalLayout();
        mainContainer.setMaxWidth("680px");
        mainContainer.setWidthFull();
        mainContainer.setPadding(true);
        mainContainer.setSpacing(true);
        mainContainer.addClassName("creation-card");
        mainContainer.getStyle()
            .set("background", "#ffffff")
            .set("border", "1px solid #d7dce2")
            .set("border-radius", "14px")
            .set("box-shadow", "0 10px 30px rgba(15, 23, 42, 0.08)")
            .set("padding", "28px");

        // Header with logout button
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.getStyle().set("margin-bottom", "20px");

        H2 logo = new H2("📱 Readit");
        logo.getStyle()
            .set("margin", "0")
            .set("color", "#ff4500")
            .set("cursor", "pointer");
        logo.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("feed")));

        Button logoutButton = new Button("Logout", VaadinIcon.SIGN_OUT.create());
        logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        logoutButton.getStyle()
            .set("color", "#334155")
            .set("font-weight", "600")
            .set("padding", "8px 16px");
        logoutButton.addClickListener(e -> handleLogout());

        headerLayout.add(logo, logoutButton);

        H2 pageTitle = new H2("Create a subreddit");
        pageTitle.getStyle()
            .set("color", "#111827")
            .set("margin", "4px 0 0");

        Paragraph pageDescription = new Paragraph(
                "Give your community a clear name and description so people know what belongs there.");
        pageDescription.getStyle()
            .set("color", "#52606d")
            .set("margin", "-8px 0 8px");

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
        create.getStyle()
            .set("background", "#ff4500")
            .set("font-weight", "700");

        Button cancel = new Button("Cancel", e -> getUI().ifPresent(ui -> ui.navigate("feed")));
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout actions = new HorizontalLayout(cancel, create);
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.END);

        mainContainer.add(headerLayout, pageTitle, pageDescription, name, description, isPrivate, actions);
        add(mainContainer);
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
}
