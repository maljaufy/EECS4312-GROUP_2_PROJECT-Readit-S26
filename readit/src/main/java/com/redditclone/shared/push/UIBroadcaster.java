package com.redditclone.shared.push;
import com.vaadin.flow.component.UI;
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
        registeredUIs.put(ui.getSession().getSession().getId(), ui);
    }

    /**
     * Unregister a UI from broadcasting.
     */
    public void unregister(UI ui) {
        registeredUIs.remove(ui.getSession().getSession().getId());
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
        UI ui = registeredUIs.get(sessionId);
        if (ui != null && ui.getSession() != null && ui.isAttached()) {
            ui.access(() -> action.accept(ui));
        }
    }
}
