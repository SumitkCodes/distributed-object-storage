package com.example.minis3.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Represents a storage node in the distributed system.
 * Each node handles file storage and retrieval operations.
 */
@Entity
@Table(name = "storage_nodes")
public class StorageNode {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Node name is required")
    @Size(min = 2, max = 50, message = "Node name must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9._-]*[a-zA-Z0-9]$", 
             message = "Node name must contain only letters, numbers, dots, underscores, and hyphens")
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @NotBlank(message = "Base URL is required")
    @Pattern(regexp = "^https?://[a-zA-Z0-9.-]+(:[0-9]+)?(/.*)?$", 
             message = "Base URL must be a valid HTTP/HTTPS URL")
    @Column(nullable = false, length = 255)
    private String baseUrl;

    @NotNull(message = "Status is required")
    @Column(nullable = false, length = 10)
    private String status = "UP"; // UP, DOWN, MAINTENANCE

    @Column(nullable = false)
    private Instant lastHeartbeat = Instant.now();

    // Default constructor for JPA
    public StorageNode() {}

    public StorageNode(String name, String baseUrl) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.status = "UP";
        this.lastHeartbeat = Instant.now();
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
    
    public String getBaseUrl() { 
        return baseUrl; 
    }
    
    public void setBaseUrl(String baseUrl) { 
        this.baseUrl = baseUrl; 
    }
    
    public String getStatus() { 
        return status; 
    }
    
    public void setStatus(String status) { 
        this.status = status; 
    }
    
    public Instant getLastHeartbeat() { 
        return lastHeartbeat; 
    }
    
    public void setLastHeartbeat(Instant lastHeartbeat) { 
        this.lastHeartbeat = lastHeartbeat; 
    }

    @Override
    public String toString() {
        return "StorageNode{id=" + id + ", name='" + name + "', baseUrl='" + baseUrl + 
               "', status='" + status + "', lastHeartbeat=" + lastHeartbeat + "}";
    }
}
