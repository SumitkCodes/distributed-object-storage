package com.example.minis3.repo;

import com.example.minis3.model.ObjectEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ObjectEntryRepo extends JpaRepository<ObjectEntry, Long> {
  Optional<ObjectEntry> findByBucketIdAndObjectKey(Long bucketId, String objectKey);
  List<ObjectEntry> findByBucketId(Long bucketId);
  List<ObjectEntry> findByBucketIdAndObjectKeyStartingWith(Long bucketId, String prefix);
}
