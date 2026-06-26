package com.example.pricing_calculation.service;

import com.example.pricing_calculation.dto.WebSocketEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RealtimeEventService {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeEventService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(String topic, String type, String message, Object payload) {
        messagingTemplate.convertAndSend(topic, WebSocketEvent.of(type, message, payload));
    }
}
