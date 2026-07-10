package com.redditclone.voting.listener;
import com.redditclone.shared.push.UIBroadcaster;
import com.redditclone.user.service.UserService;
import com.redditclone.voting.event.VoteCastEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteEventListener {

    private final UserService userService;
    private final UIBroadcaster uiBroadcaster;

    @Async
    @EventListener
    public void handleVoteCast(VoteCastEvent event) {
        log.debug("Processing VoteCastEvent for post {} by user {}", event.getPostId(), event.getUsername());

        // Recalculate karma for the post author
        // We need the post author ID – we'll get it from the post service
        // For now, we assume we have a way to get the author ID from the post

        // Broadcast karma update to all connected UIs
        uiBroadcaster.broadcast(ui -> {
            // The UI will handle refreshing the karma display
            // This will be implemented in ProfileView
            ui.getPage().executeJs(
                    "console.log('Karma updated for user: " + event.getUsername() + "')"
            );
        });
    }
}
