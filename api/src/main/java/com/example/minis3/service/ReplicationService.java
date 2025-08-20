package com.example.minis3.service;

import com.example.minis3.model.StorageNode;
import com.example.minis3.repo.StorageNodeRepo;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReplicationService {
  private final PlacementService placement;
  private final StorageNodeRepo nodeRepo;
  private final StorageNodeClient client;

  public ReplicationService(PlacementService placement, StorageNodeRepo nodeRepo, StorageNodeClient client) {
    this.placement = placement;
    this.nodeRepo = nodeRepo;
    this.client = client;
  }

  public List<Map<String, Object>> replicateUpload(String bucket, Long objectId, Long version, byte[] content, int rf) {
    String objectKey = objectId + "." + version;
    var nodes = placement.selectReplicas(bucket, objectKey, rf);
    String relPath = bucket + "/" + objectId + "/" + version + "/blob";

    List<Map<String, Object>> locs = new ArrayList<>();
    ByteArrayResource resource = new ByteArrayResource(content) {
      @Override public String getFilename() { return "blob"; }
    };

    for (StorageNode n : nodes) {
      client.store(n, resource, relPath);
      locs.add(Map.of("nodeId", n.getId(), "path", relPath));
    }
    return locs;
  }

  public ResponseEntity<byte[]> fetch(String bucket, Long objectId, Long version, List<Map<String,Object>> locs) {
    for (Map<String, Object> m : locs) {
      Long nodeId = ((Number)m.get("nodeId")).longValue();
      String path = (String)m.get("path");
      var node = nodeRepo.findById(nodeId).orElse(null);
      if (node == null) continue;
      try {
        byte[] data = client.fetch(node, path);
        return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .body(data);
      } catch (Exception ignored) { }
    }
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
  }
}
