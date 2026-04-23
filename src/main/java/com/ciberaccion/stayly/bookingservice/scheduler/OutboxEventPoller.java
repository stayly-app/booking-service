package com.ciberaccion.stayly.bookingservice.scheduler;

import com.ciberaccion.stayly.bookingservice.model.OutboxEvent;
import com.ciberaccion.stayly.bookingservice.model.enums.OutboxEventStatus;
import com.ciberaccion.stayly.bookingservice.repository.OutboxEventRepository;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final SqsTemplate sqsTemplate;

    @Value("${booking.events.queue.url}")
    private String queueUrl;

    @Scheduled(fixedDelay = 5000) // cada 5 segundos
    @Transactional
    public void pollAndPublish() {

        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatus(OutboxEventStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Found {} pending outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                sqsTemplate.send(queueUrl, event.getPayload());

                event.setStatus(OutboxEventStatus.PROCESSED);
                event.setProcessedAt(LocalDateTime.now());
                outboxEventRepository.save(event);

                log.info("Published event {} to SQS", event.getId());

            } catch (Exception e) {
                log.error("Failed to publish event {} to SQS", event.getId(), e);
                event.setStatus(OutboxEventStatus.FAILED);
                outboxEventRepository.save(event);
            }
        }
    }
}