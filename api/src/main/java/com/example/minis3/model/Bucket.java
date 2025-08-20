package com.example.minis3.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "buckets")
public class Bucket {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String name;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public Instant getCreatedAt() { return createdAt; }
}
