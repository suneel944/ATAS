package com.atas.framework.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Represents an attachment associated with a test result or step. Attachments can be screenshots,
 * videos, logs or any other type of file. Each attachment stores its URL (e.g. S3 presigned link)
 * and metadata such as MIME type and creation timestamp.
 */
@Entity
@Table(name = "test_attachments")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "result_id")
  TestResult result;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "step_id")
  TestStep step;

  @Enumerated(EnumType.STRING)
  @Column(name = "type")
  AttachmentType type;

  @Column(name = "file_name")
  String fileName;

  @Column(name = "mime_type")
  String mimeType;

  @Column(name = "url")
  String url;

  @Column(name = "created_at")
  LocalDateTime createdAt;
}
