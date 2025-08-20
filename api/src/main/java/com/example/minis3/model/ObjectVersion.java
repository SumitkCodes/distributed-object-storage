package com.example.minis3.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "object_versions", indexes = {
  @Index(columnList = "objectEntry_id, version", unique = true)
})
public class ObjectVersion {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private ObjectEntry objectEntry;

  @Column(nullable = false)
  private Long version;

  private Long sizeBytes;

  @Column(length = 128)
  private String checksum;

  @Column(columnDefinition = "TEXT")
  private String locationsJson;

  @Column(nullable = false)
  private boolean deleted = false;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public ObjectEntry getObjectEntry() { return objectEntry; }
  public void setObjectEntry(ObjectEntry objectEntry) { this.objectEntry = objectEntry; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
  public Long getSizeBytes() { return sizeBytes; }
  public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
  public String getChecksum() { return checksum; }
  public void setChecksum(String checksum) { this.checksum = checksum; }
  public String getLocationsJson() { return locationsJson; }
  public void setLocationsJson(String locationsJson) { this.locationsJson = locationsJson; }
  public boolean isDeleted() { return deleted; }
  public void setDeleted(boolean deleted) { this.deleted = deleted; }
  public Instant getCreatedAt() { return createdAt; }
}
