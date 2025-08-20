package com.example.minis3.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Represents a storage bucket - a logical container for objects.
 * Buckets provide namespace isolation and can have different policies.
 */
@Entity
@Table(name = "buckets")
public class Bucket {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Bucket name is required")
    @Size(min = 3, max = 63, message = "Bucket name must be between 3 and 63 characters")
    @Pattern(regexp = "^[a-z0-9][a-z0-9.-]*[a-z0-9]$", 
             message = "Bucket name must contain only lowercase letters, numbers, dots, and hyphens")
    @Column(unique = true, nullable = false, length = 63)
    private String name;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Default constructor for JPA
    public Bucket() {}

    public Bucket(String name) {
        this.name = name;
        this.createdAt = Instant.now();
    }

    // Getters and setters
    public Long getId() { 
        return id; 
    }
    
    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }
    
    public Instant getCreatedAt() { 
        return createdAt; 
    }

    @Override
    public String toString() {
        return "Bucket{id=" + id + ", name='" + name + "', createdAt=" + createdAt + "}";
    }
}
