package com.redditclone.subreddit.ui;

import com.redditclone.subreddit.service.SubredditService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("create-subreddit")
@PageTitle("Create Subreddit | Reddit Clone")
public class CreateSubredditView extends VerticalLayout {

    public CreateSubredditView(SubredditService subredditService) {
        setPadding(true);
        setSpacing(true);
        setMaxWidth("600px");

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

        Button cancel = new Button("Cancel", e -> getUI().ifPresent(ui -> ui.navigate("feed")));

        add(new H2("Create a subreddit"), name, description, isPrivate,
                new HorizontalLayout(create, cancel));
    }
}
