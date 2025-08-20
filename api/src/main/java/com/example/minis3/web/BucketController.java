package com.example.minis3.web;

import com.example.minis3.model.Bucket;
import com.example.minis3.repo.BucketRepo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/buckets")
public class BucketController {
  private final BucketRepo bucketRepo;

  public BucketController(BucketRepo bucketRepo) {
    this.bucketRepo = bucketRepo;
  }

  @PostMapping
  public Bucket create(@RequestParam String name) {
    bucketRepo.findByName(name).ifPresent(b -> {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Bucket exists");
    });
    Bucket b = new Bucket();
    b.setName(name);
    return bucketRepo.save(b);
  }

  @GetMapping
  public List<Bucket> list() {
    return bucketRepo.findAll();
  }
}
