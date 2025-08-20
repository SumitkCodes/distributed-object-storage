package com.example.minis3.repo;

import com.example.minis3.model.StorageNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StorageNodeRepo extends JpaRepository<StorageNode, Long> {
  List<StorageNode> findByStatusIgnoreCase(String status);
  Optional<StorageNode> findByName(String name);
}
