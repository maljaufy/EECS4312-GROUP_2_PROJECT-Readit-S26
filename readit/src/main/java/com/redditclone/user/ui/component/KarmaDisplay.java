package com.redditclone.user.ui.component;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class KarmaDisplay extends  Composite<HorizontalLayout> {

    /*
    Karma display: Main karma display: User karma display
    i.e. Reusable karma display component
    */

    private final Span karmaValue = new Span();

    public KarmaDisplay(int karma) {
        getContent().setSpacing(true);
        getContent().setAlignItems(Alignment.CENTER);

        Icon karmaIcon = VaadinIcon.STAR.create();
        karmaIcon.getStyle().set("color", "var(--lumo-primary-color)");
        karmaIcon.setSize("16px");

        updateKarma(karma);

        getContent().add(karmaIcon, karmaValue);
    }

    public void updateKarma(int karma) {
        karmaValue.setText(String.valueOf(karma));
        if (karma > 0) {
            karmaValue.getStyle().set("color", "var(--lumo-success-color)");
            karmaValue.getStyle().set("font-weight", "bold");
        } else if (karma < 0) {
            karmaValue.getStyle().set("color", "var(--lumo-error-color)");
            karmaValue.getStyle().set("font-weight", "bold");
        } else {
            karmaValue.getStyle().set("color", "var(--lumo-secondary-text-color)");
            karmaValue.getStyle().set("font-weight", "normal");
        }
    }



}
