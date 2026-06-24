package com.redditclone.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {

    /*
    Async configuration: Main async configuration - Enables @Async for event listener
    */

    // This enables @Async for event listeners
    // Events will be processed asynchronously without blocking the main transaction
}
