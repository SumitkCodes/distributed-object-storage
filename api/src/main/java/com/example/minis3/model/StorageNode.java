package com.example.minis3.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "storage_nodes")
public class StorageNode {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false)
  private String baseUrl; // e.g., http://node1:9091

  @Column(nullable = false)
  private String status = "UP"; // UP/DOWN

  private Instant lastHeartbeat = Instant.now();

  public Long getId() { return id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getBaseUrl() { return baseUrl; }
  public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Instant getLastHeartbeat() { return lastHeartbeat; }
  public void setLastHeartbeat(Instant lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
}
