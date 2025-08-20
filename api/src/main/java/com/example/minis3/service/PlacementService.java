package com.example.minis3.service;

import com.example.minis3.model.StorageNode;
import com.example.minis3.repo.StorageNodeRepo;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;

@Service
public class PlacementService {
  private final StorageNodeRepo nodeRepo;

  public PlacementService(StorageNodeRepo nodeRepo) {
    this.nodeRepo = nodeRepo;
  }

  public List<StorageNode> selectReplicas(String bucket, String objectKey, int rf) {
    List<StorageNode> up = nodeRepo.findByStatusIgnoreCase("UP");
    if (up.size() < rf) throw new IllegalStateException("Insufficient UP nodes for replication=" + rf);

    String key = bucket + "/" + objectKey;
    List<Map.Entry<StorageNode, Double>> scored = new ArrayList<>();
    for (StorageNode n : up) {
      double s = score(key, n.getName());
      scored.add(Map.entry(n, s));
    }
    scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
    List<StorageNode> chosen = new ArrayList<>();
    for (int i = 0; i < rf; i++) chosen.add(scored.get(i).getKey());
    return chosen;
  }

  private double score(String key, String nodeName) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest((key + "@" + nodeName).getBytes());
      long l = ByteBuffer.wrap(digest, 0, 8).getLong();
      long u = l ^ (l >>> 1);
      double d = (u >>> 11) * (1.0 / (1L << 53)); // [0,1)
      return d;
    } catch (Exception e) {
      return Math.random(); // fallback (shouldn't happen)
    }
  }
}
