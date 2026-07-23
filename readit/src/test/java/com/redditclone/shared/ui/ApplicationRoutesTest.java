package com.redditclone.shared.ui;

import com.redditclone.comments.ui.PostCommentsView;
import com.redditclone.notification.ui.CircuitBreakerDashboardView;
import com.redditclone.posts.ui.CreatePostView;
import com.redditclone.posts.ui.EditPostView;
import com.redditclone.posts.ui.FeedView;
import com.redditclone.posts.ui.PopularView;
import com.redditclone.posts.ui.PostDetailView;
import com.redditclone.subreddit.ui.AllSubredditsView;
import com.redditclone.subreddit.ui.CreateSubredditView;
import com.redditclone.subreddit.ui.SubredditView;
import com.redditclone.user.ui.LoginView;
import com.redditclone.user.ui.NotificationPreferencesView;
import com.redditclone.user.ui.ProfileView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.OptionalParameter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApplicationRoutesTest {

    @Test
    void loginRemainsAvailableAtTheRootAndLoginPaths() {
        Route route = LoginView.class.getAnnotation(Route.class);
        RouteAlias alias = LoginView.class.getAnnotation(RouteAlias.class);

        assertNotNull(route);
        assertEquals("", route.value());
        assertNotNull(alias);
        assertEquals("login", alias.value());
    }

    @Test
    void contentPagesUseThePersistentApplicationLayout() {
        List<Class<? extends Component>> contentViews = List.of(
                FeedView.class,
                PopularView.class,
                PostDetailView.class,
                PostCommentsView.class,
                CreatePostView.class,
                EditPostView.class,
                RecommendationsView.class,
                SubredditView.class,
                AllSubredditsView.class,
                CreateSubredditView.class,
                ProfileView.class,
                NotificationPreferencesView.class,
                CircuitBreakerDashboardView.class
        );

        for (Class<? extends Component> view : contentViews) {
            Route route = view.getAnnotation(Route.class);
            assertNotNull(route, () -> view.getSimpleName() + " must remain routable");
            assertEquals(MainLayout.class, route.layout(),
                    () -> view.getSimpleName() + " must keep the left sidebar");
        }
    }

    @Test
    void profileCanBeOpenedFromTheSidebarWithoutAUsernameParameter() throws Exception {
        var parameter = ProfileView.class
                .getMethod("setParameter", com.vaadin.flow.router.BeforeEvent.class, String.class)
                .getParameters()[1];

        assertNotNull(parameter.getAnnotation(OptionalParameter.class));
    }
}
