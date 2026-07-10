package com.redditclone.shared.push;
import com.vaadin.flow.component.UI;
import elemental.json.Json;
import elemental.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

@Component
@Slf4j
public class UIBroadcaster implements Serializable {

    private final ConcurrentMap<String, UI> registeredUIs = new ConcurrentHashMap<>();

    /**
     * Register a UI for broadcasting.
     */
    public void register(UI ui) {
        registeredUIs.put(uiKey(ui), ui);
    }

    /**
     * Unregister a UI from broadcasting.
     */
    public void unregister(UI ui) {
        registeredUIs.remove(uiKey(ui));
    }

    /**
     * Broadcasts an action to all currently open UIs.
     */
    public void broadcast(Consumer<UI> action) {
        registeredUIs.forEach((sessionId, ui) -> {
            if (ui.getSession() != null && ui.isAttached()) {
                ui.access(() -> {
                    try {
                        action.accept(ui);
                    } catch (Exception e) {
                        log.error("Error broadcasting to UI", e);
                    }
                });
            }
        });
    }

    /**
     * Broadcasts to a specific UI by session ID.
     */
    public void broadcastToSession(String sessionId, Consumer<UI> action) {
        registeredUIs.forEach((key, ui) -> {
            if (key.startsWith(sessionId + ":") && ui.getSession() != null && ui.isAttached()) {
                ui.access(() -> action.accept(ui));
            }
        });
    }

    /**
     * Sends a vote update to every open Vaadin UI.  The browser event keeps the
     * broadcaster independent of individual views: a view that renders a vote
     * score can subscribe to {@code readit-vote-updated} and refresh only the
     * matching target.
     */
    public void broadcastVoteUpdate(String targetType, Long targetId, int score) {
        JsonObject detail = Json.createObject();
        detail.put("targetType", targetType);
        detail.put("targetId", targetId);
        detail.put("score", score);
        broadcast(ui -> ui.getPage().executeJs(
                "window.dispatchEvent(new CustomEvent('readit-vote-updated', { detail: $0 }));",
                detail
        ));
    }

    private String uiKey(UI ui) {
        return ui.getSession().getSession().getId() + ":" + ui.getUIId();
    }
}
