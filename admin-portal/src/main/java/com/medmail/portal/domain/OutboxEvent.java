package com.medmail.portal.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "outbox_event", indexes = {
    @Index(name = "idx_outbox_processed_at", columnList = "processed_at")
})
@Getter @Setter
public class OutboxEvent {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "aggregate_type", nullable = false, length = 100)
  private String aggregateType; // "record"

  @Column(name = "aggregate_id", nullable = false)
  private Long aggregateId;     // record id

  @Column(name = "event_type", nullable = false, length = 100)
  private String eventType;     // "RecordReceived"

  @Column(name = "payload_json", columnDefinition = "JSON", nullable = false)
  private String payloadJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "processed_at")
  private Instant processedAt;
}
