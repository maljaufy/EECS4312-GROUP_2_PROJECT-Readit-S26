package com.redditclone.posts.ui;

import com.redditclone.posts.domain.Post;
import com.redditclone.posts.service.PostService;
import com.redditclone.shared.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.Optional;

@Route(value = "post/:postId/edit", layout = MainLayout.class)
@PageTitle("Edit Post | Reddit Clone")
public class EditPostView extends VerticalLayout implements BeforeEnterObserver {

    private final PostService postService;

    private Post post;

    public EditPostView(PostService postService) {
        this.postService = postService;

        setPadding(true);
        setSpacing(true);
        setMaxWidth("700px");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();

        Optional<String> postIdParam = event.getRouteParameters().get("postId");
        if (postIdParam.isEmpty()) {
            showMessage("Post not found");
            return;
        }

        long postId;
        try {
            postId = Long.parseLong(postIdParam.get());
        } catch (NumberFormatException invalidId) {
            showMessage("Post not found");
            return;
        }

        try {
            this.post = postService.getPostById(postId);
        } catch (IllegalArgumentException notFound) {
            showMessage("Post not found");
            return;
        }

        Long userId = currentUserId();
        if (userId == null) {
            showMessage("Please log in to edit this post.");
            return;
        }
        if (!postService.isAuthor(postId, userId)) {
            showMessage("You can only edit your own posts.");
            return;
        }

        buildForm();
    }

    private void buildForm() {
        TextField title = new TextField("Title");
        title.setValue(post.getTitle() == null ? "" : post.getTitle());
        title.setRequiredIndicatorVisible(true);
        title.setMaxLength(300);
        title.setWidthFull();

        TextArea content = new TextArea("Content");
        content.setValue(post.getContent() == null ? "" : post.getContent());
        content.setWidthFull();
        content.setMinHeight("200px");

        Span community = new Span("Posting in r/" + post.getSubreddit().getName());
        community.getStyle()
                .set("color", "#7c7c7c")
                .set("font-size", "13px");

        Button save = new Button("Save changes", event -> save(title.getValue(), content.getValue()));
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", event ->
                getUI().ifPresent(ui -> ui.navigate("post/" + post.getId())));

        add(new H2("Edit post"), community, title, content, new HorizontalLayout(save, cancel));
    }

    private void save(String title, String content) {
        try {
            postService.updatePost(post.getId(), title, content, currentUserId());
            Notification ok = Notification.show("Post updated.");
            ok.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            getUI().ifPresent(ui -> ui.navigate("post/" + post.getId()));
        } catch (IllegalArgumentException | IllegalStateException failure) {
            Notification error = Notification.show(failure.getMessage());
            error.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private Long currentUserId() {
        return (Long) getUI()
                .map(ui -> ui.getSession().getAttribute("userId"))
                .orElse(null);
    }

    private void showMessage(String message) {
        add(new H2(message));
        Button backToFeed = new Button("Back to feed",
                event -> getUI().ifPresent(ui -> ui.navigate("feed")));
        backToFeed.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add(backToFeed);
    }
}
