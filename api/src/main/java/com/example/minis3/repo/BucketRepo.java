package com.example.minis3.repo;

import com.example.minis3.model.Bucket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BucketRepo extends JpaRepository<Bucket, Long> {
  Optional<Bucket> findByName(String name);
}
