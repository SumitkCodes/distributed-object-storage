package com.example.minis3.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "objects", indexes = {
  @Index(columnList = "bucket_id, objectKey", unique = true)
})
public class ObjectEntry {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Bucket bucket;

  @Column(nullable = false)
  private String objectKey;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private Long nextVersion = 1L;

  public Long getId() { return id; }
  public Bucket getBucket() { return bucket; }
  public void setBucket(Bucket bucket) { this.bucket = bucket; }
  public String getObjectKey() { return objectKey; }
  public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
  public Instant getCreatedAt() { return createdAt; }
  public Long getNextVersion() { return nextVersion; }
  public void setNextVersion(Long nextVersion) { this.nextVersion = nextVersion; }
}
