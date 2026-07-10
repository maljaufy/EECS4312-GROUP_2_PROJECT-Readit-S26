package com.redditclone.shared.config;

import com.redditclone.shared.push.UIBroadcaster;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

@Component
public class VaadinUIConfig implements VaadinServiceInitListener {

    private final UIBroadcaster uiBroadcaster;

    public VaadinUIConfig(UIBroadcaster uiBroadcaster) {
        this.uiBroadcaster = uiBroadcaster;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(new UIInitListener() {
            @Override
            public void uiInit(UIInitEvent event) {
                event.getUI().addDetachListener(detachEvent -> {
                    uiBroadcaster.unregister(event.getUI());
                });
                uiBroadcaster.register(event.getUI());
            }
        });
    }
}
