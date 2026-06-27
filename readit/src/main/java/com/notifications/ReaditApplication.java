package com.notifications;

import com.notifications.commentEventProducer;
import com.notifications.postEventProducer;
import com.notifications.voteEventProducer;
import com.notifications.voteEvent;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ReaditApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReaditApplication.class, args);
    }
    @Bean
    CommandLineRunner test(
            postEventProducer postProducer,
            voteEventProducer voteProducer,
            commentEventProducer commentProducer) {
        return args -> {
            postProducer.publishCreated("user-1", "Test Post", "Hello Kafka");
            voteProducer.publishUpvote("post-123", voteEvent.TargetType.POST, "user-2", 1);
            commentProducer.publishCreated("post-123", "user-3", "Great post!", null);
            commentProducer.publishCreated("post-123", "user-4", "I agree!", "comment-abc");
        };
    }
}