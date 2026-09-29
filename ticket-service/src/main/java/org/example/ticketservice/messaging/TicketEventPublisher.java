package org.example.ticketservice.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketEventPublisher {

    private static final String TICKET_CREATED_TOPIC = "ticket-created";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publishTicketCreated(String passengerEmail) {
        kafkaTemplate.send(TICKET_CREATED_TOPIC, passengerEmail);
    }
}