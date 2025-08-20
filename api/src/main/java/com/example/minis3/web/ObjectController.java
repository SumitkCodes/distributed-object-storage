package com.example.minis3.web;

import com.example.minis3.model.*;
import com.example.minis3.repo.*;
import com.example.minis3.service.ReplicationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.util.*;

@RestController
@RequestMapping("/objects")
public class ObjectController {
  private final BucketRepo bucketRepo;
  private final ObjectEntryRepo entryRepo;
  private final ObjectVersionRepo versionRepo;
  private final ReplicationService repl;
  private final ObjectMapper mapper = new ObjectMapper();

  public ObjectController(BucketRepo bucketRepo, ObjectEntryRepo entryRepo, ObjectVersionRepo versionRepo, ReplicationService repl) {
    this.bucketRepo = bucketRepo;
    this.entryRepo = entryRepo;
    this.versionRepo = versionRepo;
    this.repl = repl;
  }

  @PostMapping(path="/{bucket}/{key}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String,Object> upload(@PathVariable String bucket, @PathVariable String key,
                                   @RequestPart("file") MultipartFile file) throws Exception {
    Bucket b = bucketRepo.findByName(bucket)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bucket not found"));

    if (StringUtils.isBlank(key)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Key required");

    ObjectEntry oe = entryRepo.findByBucketIdAndObjectKey(b.getId(), key)
      .orElseGet(() -> {
        ObjectEntry x = new ObjectEntry();
        x.setBucket(b);
        x.setObjectKey(key);
        return entryRepo.save(x);
      });

    long version = oe.getNextVersion();
    oe.setNextVersion(version + 1);
    entryRepo.save(oe);

    byte[] content = file.getBytes();
    String checksum = sha256Hex(content);

    var locs = repl.replicateUpload(bucket, oe.getId(), version, content, 2);

    ObjectVersion ov = new ObjectVersion();
    ov.setObjectEntry(oe);
    ov.setVersion(version);
    ov.setSizeBytes((long)content.length);
    ov.setChecksum(checksum);
    ov.setLocationsJson(mapper.writeValueAsString(locs));
    versionRepo.save(ov);

    return Map.of("bucket", bucket, "key", key, "version", version, "checksum", checksum);
  }

  @GetMapping("/{bucket}/{key}")
  public ResponseEntity<byte[]> download(@PathVariable String bucket, @PathVariable String key,
                                         @RequestParam(required = false) Long version) throws Exception {
    Bucket b = bucketRepo.findByName(bucket)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bucket not found"));

    ObjectEntry oe = entryRepo.findByBucketIdAndObjectKey(b.getId(), key)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Object not found"));

    ObjectVersion ov;
    if (version == null) {
      ov = versionRepo.findTopByObjectEntryIdAndDeletedFalseOrderByVersionDesc(oe.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No versions"));
    } else {
      ov = versionRepo.findByObjectEntryIdAndVersion(oe.getId(), version)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));
      if (ov.isDeleted()) throw new ResponseStatusException(HttpStatus.GONE, "Version deleted");
    }

    List<Map<String,Object>> locs = mapper.readValue(ov.getLocationsJson(), new TypeReference<>(){});
    return repl.fetch(bucket, oe.getId(), ov.getVersion(), locs);
  }

  @DeleteMapping("/{bucket}/{key}")
  public Map<String,Object> deleteVersion(@PathVariable String bucket, @PathVariable String key,
                                          @RequestParam Long version) {
    Bucket b = bucketRepo.findByName(bucket)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bucket not found"));
    ObjectEntry oe = entryRepo.findByBucketIdAndObjectKey(b.getId(), key)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Object not found"));
    ObjectVersion ov = versionRepo.findByObjectEntryIdAndVersion(oe.getId(), version)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));
    ov.setDeleted(true);
    versionRepo.save(ov);
    return Map.of("deleted", true, "bucket", bucket, "key", key, "version", version);
  }

  @GetMapping("/{bucket}")
  public List<Map<String,Object>> listObjects(@PathVariable String bucket,
                                              @RequestParam(required=false) String prefix) {
    Bucket b = bucketRepo.findByName(bucket)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bucket not found"));
    List<ObjectEntry> entries = (prefix == null || prefix.isEmpty())
      ? entryRepo.findByBucketId(b.getId())
      : entryRepo.findByBucketIdAndObjectKeyStartingWith(b.getId(), prefix);
    List<Map<String,Object>> out = new ArrayList<>();
    for (ObjectEntry e : entries) {
      out.add(Map.of("key", e.getObjectKey(), "id", e.getId(), "createdAt", e.getCreatedAt()));
    }
    return out;
  }

  private static String sha256Hex(byte[] data) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] digest = md.digest(data);
    StringBuilder sb = new StringBuilder();
    for (byte b : digest) sb.append(String.format("%02x", b));
    return sb.toString();
  }
}
