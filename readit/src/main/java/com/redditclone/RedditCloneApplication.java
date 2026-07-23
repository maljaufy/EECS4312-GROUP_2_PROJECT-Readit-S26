package com.redditclone;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Push
@SpringBootApplication
@EnableAsync
@Theme(value = "reddit-clone")
@EnableScheduling
public class RedditCloneApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(RedditCloneApplication.class, args);
    }
}