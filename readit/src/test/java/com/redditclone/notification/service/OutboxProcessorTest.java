package com.redditclone.notification.service;

import com.redditclone.notification.domain.OutboxEvent;
import com.redditclone.notification.repository.OutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxProcessorTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxProcessor outboxProcessor;

    @Test
    void processPendingEvents_ShouldPublishToKafka() {
        OutboxEvent event = new OutboxEvent();
        event.setId(1L);
        event.setEventType("UserRegisteredEvent");
        event.setAggregateId("123");
        event.setPayload("{\"username\":\"test\"}");
        event.setStatus("PENDING");

        when(outboxRepository.findTop100ByStatusPending()).thenReturn(List.of(event));

        outboxProcessor.processPendingEvents();

        verify(kafkaTemplate).send("user.events", "123", "{\"username\":\"test\"}");
        verify(outboxRepository).save(event);
    }
}
