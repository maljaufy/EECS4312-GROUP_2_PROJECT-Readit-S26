package com.redditclone.comments.ui;

import com.redditclone.comments.domain.Comment;
import com.redditclone.comments.dto.CommentDto;
import com.redditclone.comments.service.CommentService;
import com.redditclone.posts.domain.Post;
import com.redditclone.posts.service.PostService;
import com.redditclone.user.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Optional;

/**
 * NOTE: identity is read from the Vaadin session ("userId" attribute, set at
 * login) rather than userService.getCurrentUser() / SecurityContextHolder.
 * This is a workaround while the session-vs-JWT security config is unresolved -
 * see the "userId" attribute set in LoginView.
 */
@Route("post/:postId/comments")
@PageTitle("Comments | Reddit Clone")
public class PostCommentsView extends VerticalLayout implements BeforeEnterObserver {

    private final PostService postService;
    private final CommentService commentService;
    private final UserService userService;

    // One coordinator per page load - shared by every CommentThreadComponent in this
    // thread so that opening a reply box anywhere closes whichever one was open before.
    private final ReplyFormCoordinator replyFormCoordinator = new ReplyFormCoordinator();

    private VerticalLayout commentsContainer;
    private Post post;

    public PostCommentsView(PostService postService,
                            CommentService commentService,
                            UserService userService) {
        this.postService = postService;
        this.commentService = commentService;
        this.userService = userService;

        setPadding(true);
        setSpacing(true);
        setMaxWidth("800px");
    }

    /**
     * Reads the logged-in user's ID from the Vaadin session, or null if nobody's logged in.
     * Session attribute is set once, at login, in LoginView.
     */
    private Long getCurrentUserId() {
        return (Long) getUI()
                .map(ui -> ui.getSession().getAttribute("userId"))
                .orElse(null);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<String> postIdParam = event.getRouteParameters().get("postId");

        if (postIdParam.isEmpty()) {
            removeAll();
            add(new H2("Post not found"));
            return;
        }

        Long postId;
        try {
            postId = Long.parseLong(postIdParam.get());
        } catch (NumberFormatException invalidId) {
            removeAll();
            add(new H2("Post not found"));
            return;
        }

        try {
            this.post = postService.getPostById(postId);
        } catch (IllegalArgumentException notFound) {
            removeAll();
            add(new H2("Post not found"));
            return;
        }

        removeAll();
        add(new H2(post.getTitle()));
        if (post.getContent() != null && !post.getContent().isBlank()) {
            add(new Paragraph(post.getContent()));
        }

        add(buildNewCommentForm());
        add(new H3("Comments"));

        commentsContainer = new VerticalLayout();
        commentsContainer.setPadding(false);
        commentsContainer.setSpacing(false);
        commentsContainer.setWidthFull();
        add(commentsContainer);

        loadTopLevelComments();
    }

    private VerticalLayout buildNewCommentForm() {
        VerticalLayout form = new VerticalLayout();
        form.setPadding(false);
        form.setSpacing(true);

        TextArea body = new TextArea("Add a comment");
        body.setWidthFull();
        body.setMinHeight("100px");

        Button submit = new Button("Post comment", e -> {
            Long currentUserId = getCurrentUserId();

            if (currentUserId == null) {
                Notification error = Notification.show("Please log in first.");
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                getUI().ifPresent(ui -> ui.navigate("login"));
                return;
            }
            if (body.getValue() == null || body.getValue().isBlank()) {
                Notification error = Notification.show("Comment can't be empty.");
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                CommentDto dto = new CommentDto(post.getId(), null, body.getValue());
                commentService.createComment(dto.getPostId(), currentUserId, dto.getBody());
                body.clear();
                loadTopLevelComments();
                Notification ok = Notification.show("Comment posted.");
                ok.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalArgumentException ex) {
                Notification error = Notification.show(ex.getMessage());
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        form.add(body, new HorizontalLayout(submit));
        return form;
    }

    private void loadTopLevelComments() {
        commentsContainer.removeAll();
        List<Comment> topLevel = commentService.findTopLevelByPost(post);

        if (topLevel.isEmpty()) {
            commentsContainer.add(new Paragraph("No comments yet. Be the first to reply."));
            return;
        }

        for (Comment comment : topLevel) {
            commentsContainer.add(new CommentThreadComponent(
                    comment, 0, commentService, userService, this::loadTopLevelComments, replyFormCoordinator));
        }
    }
}