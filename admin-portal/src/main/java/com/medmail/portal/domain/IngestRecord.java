package com.medmail.portal.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "record", indexes = {
    @Index(name = "idx_record_file_id", columnList = "file_id"),
    @Index(name = "idx_record_status", columnList = "status")
})
@Getter @Setter
public class IngestRecord {

  public enum Status { RECEIVED, PROCESSING, PROCESSED, FAILED }

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "file_id", nullable = false)
  private UploadedFile file;

  @Column(name = "payload_json", columnDefinition = "JSON", nullable = false)
  private String payloadJson;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private Status status = Status.RECEIVED;

  @Column(name = "error_message")
  private String errorMessage;

  @Column(name = "pdf_url")
  private String pdfUrl;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @PreUpdate
  void touch() { this.updatedAt = Instant.now(); }
}
