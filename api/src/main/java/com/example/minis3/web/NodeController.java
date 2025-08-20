package com.example.minis3.web;

import com.example.minis3.model.StorageNode;
import com.example.minis3.repo.StorageNodeRepo;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/nodes")
public class NodeController {
  private final StorageNodeRepo repo;

  public NodeController(StorageNodeRepo repo) { this.repo = repo; }

  @PostMapping("/register")
  public StorageNode register(@RequestBody Map<String,String> body) {
    String name = body.get("name");
    String baseUrl = body.get("baseUrl");
    var node = repo.findByName(name).orElseGet(StorageNode::new);
    node.setName(name);
    node.setBaseUrl(baseUrl);
    node.setStatus("UP");
    node.setLastHeartbeat(Instant.now());
    return repo.save(node);
  }

  @GetMapping
  public List<StorageNode> list() { return repo.findAll(); }
}
