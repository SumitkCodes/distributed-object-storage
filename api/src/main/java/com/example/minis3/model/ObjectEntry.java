package com.example.minis3.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Represents a logical object entry in the system.
 * Maps to physical file versions stored across storage nodes.
 */
@Entity
@Table(name = "objects", indexes = {
    @Index(columnList = "bucket_id, objectKey", unique = true)
})
public class ObjectEntry {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Bucket is required")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Bucket bucket;

    @NotBlank(message = "Object key is required")
    @Size(max = 1024, message = "Object key must not exceed 1024 characters")
    @Column(nullable = false, length = 1024)
    private String objectKey;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Long nextVersion = 1L;

    // Default constructor for JPA
    public ObjectEntry() {}

    public ObjectEntry(Bucket bucket, String objectKey) {
        this.bucket = bucket;
        this.objectKey = objectKey;
        this.createdAt = Instant.now();
        this.nextVersion = 1L;
    }

    // Getters and setters
    public Long getId() { 
        return id; 
    }
    
    public Bucket getBucket() { 
        return bucket; 
    }
    
    public void setBucket(Bucket bucket) { 
        this.bucket = bucket; 
    }
    
    public String getObjectKey() { 
        return objectKey; 
    }
    
    public void setObjectKey(String objectKey) { 
        this.objectKey = objectKey; 
    }
    
    public Instant getCreatedAt() { 
        return createdAt; 
    }
    
    public Long getNextVersion() { 
        return nextVersion; 
    }
    
    public void setNextVersion(Long nextVersion) { 
        this.nextVersion = nextVersion; 
    }

    @Override
    public String toString() {
        return "ObjectEntry{id=" + id + ", bucketId=" + (bucket != null ? bucket.getId() : "null") + 
               ", objectKey='" + objectKey + "', createdAt=" + createdAt + ", nextVersion=" + nextVersion + "}";
    }
}
