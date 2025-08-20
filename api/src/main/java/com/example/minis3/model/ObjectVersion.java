package com.example.minis3.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Represents a specific version of an object.
 * Each version contains metadata about the file and its storage locations.
 */
@Entity
@Table(name = "object_versions", indexes = {
    @Index(columnList = "objectEntry_id, version", unique = true)
})
public class ObjectVersion {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Object entry is required")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private ObjectEntry objectEntry;

    @NotNull(message = "Version number is required")
    @Positive(message = "Version number must be positive")
    @Column(nullable = false)
    private Long version;

    @Positive(message = "File size must be positive")
    private Long sizeBytes;

    @Size(max = 128, message = "Checksum must not exceed 128 characters")
    @Column(length = 128)
    private String checksum;

    @Column(columnDefinition = "TEXT")
    private String locationsJson;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Default constructor for JPA
    public ObjectVersion() {}

    public ObjectVersion(ObjectEntry objectEntry, Long version) {
        this.objectEntry = objectEntry;
        this.version = version;
        this.createdAt = Instant.now();
        this.deleted = false;
    }

    // Getters and setters
    public Long getId() { 
        return id; 
    }
    
    public ObjectEntry getObjectEntry() { 
        return objectEntry; 
    }
    
    public void setObjectEntry(ObjectEntry objectEntry) { 
        this.objectEntry = objectEntry; 
    }
    
    public Long getVersion() { 
        return version; 
    }
    
    public void setVersion(Long version) { 
        this.version = version; 
    }
    
    public Long getSizeBytes() { 
        return sizeBytes; 
    }
    
    public void setSizeBytes(Long sizeBytes) { 
        this.sizeBytes = sizeBytes; 
    }
    
    public String getChecksum() { 
        return checksum; 
    }
    
    public void setChecksum(String checksum) { 
        this.checksum = checksum; 
    }
    
    public String getLocationsJson() { 
        return locationsJson; 
    }
    
    public void setLocationsJson(String locationsJson) { 
        this.locationsJson = locationsJson; 
    }
    
    public boolean isDeleted() { 
        return deleted; 
    }
    
    public void setDeleted(boolean deleted) { 
        this.deleted = deleted; 
    }
    
    public Instant getCreatedAt() { 
        return createdAt; 
    }

    @Override
    public String toString() {
        return "ObjectVersion{id=" + id + ", objectEntryId=" + 
               (objectEntry != null ? objectEntry.getId() : "null") + 
               ", version=" + version + ", sizeBytes=" + sizeBytes + 
               ", checksum='" + checksum + "', deleted=" + deleted + 
               ", createdAt=" + createdAt + "}";
    }
}
