package com.medmail.portal.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "uploaded_file")
@Getter @Setter
public class UploadedFile {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "original_name", nullable = false, length = 255)
  private String originalName;

  @Column(name = "content_type", length = 100)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  // com.medmail.portal.domain.UploadedFile
// ...
  @Column(name = "sha256", columnDefinition = "char(64)", nullable = false)
  private String sha256;


  @Column(name = "uploaded_at", nullable = false)
  private Instant uploadedAt = Instant.now();

  @Lob
  @Column(name = "raw_json", columnDefinition = "LONGTEXT", nullable = false)
  private String rawJson;
}
