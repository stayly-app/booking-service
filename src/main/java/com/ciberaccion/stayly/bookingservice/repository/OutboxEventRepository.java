package com.ciberaccion.stayly.bookingservice.repository;

import com.ciberaccion.stayly.bookingservice.model.OutboxEvent;
import com.ciberaccion.stayly.bookingservice.model.enums.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatus(OutboxEventStatus status);
}