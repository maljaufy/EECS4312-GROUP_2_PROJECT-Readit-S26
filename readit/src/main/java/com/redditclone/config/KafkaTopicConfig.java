package com.redditclone.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic postEventsTopic() {
        return new NewTopic("post.events", 1, (short) 1);
    }

    @Bean
    public NewTopic voteEventsTopic() {
        return new NewTopic("vote.events", 1, (short) 1);
    }

    @Bean
    public NewTopic commentEventsTopic() {
        return new NewTopic("comment.events", 1, (short) 1);
    }

    @Bean
    public NewTopic notificationEventsTopic() {
        return new NewTopic("notification.events", 1, (short) 1);
    }

    @Bean
    public NewTopic userEventsTopic() {
        return new NewTopic("user.events", 1, (short) 1);
    }


}
