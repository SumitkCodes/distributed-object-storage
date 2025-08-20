package com.example.minis3.repo;

import com.example.minis3.model.ObjectVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ObjectVersionRepo extends JpaRepository<ObjectVersion, Long> {
  Optional<ObjectVersion> findTopByObjectEntryIdAndDeletedFalseOrderByVersionDesc(Long objectEntryId);
  Optional<ObjectVersion> findByObjectEntryIdAndVersion(Long objectEntryId, Long version);
  List<ObjectVersion> findByObjectEntryIdAndDeletedFalseOrderByVersionAsc(Long objectEntryId);
}
