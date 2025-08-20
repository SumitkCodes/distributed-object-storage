package com.example.storagenode.web;

import com.example.storagenode.util.PathUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;

@RestController
public class StorageController {

  private final Path baseDir;

  public StorageController(@Value("${storage.base-dir:./data}") String base) {
    this.baseDir = Paths.get(base).toAbsolutePath().normalize();
  }

  @PutMapping(path="/store", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<String> store(@RequestPart("file") MultipartFile file,
                                      @RequestPart("path") String relPath) throws Exception {
    Path target = PathUtil.safeResolve(baseDir, relPath);
    Files.createDirectories(target.getParent());
    try (InputStream in = file.getInputStream()) {
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }
    return ResponseEntity.ok("OK");
  }

  @GetMapping("/fetch")
  public ResponseEntity<InputStreamResource> fetch(@RequestParam("path") String relPath) throws Exception {
    Path target = PathUtil.safeResolve(baseDir, relPath);
    if (!Files.exists(target) || !Files.isRegularFile(target)) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
    InputStream in = Files.newInputStream(target, StandardOpenOption.READ);
    return ResponseEntity.ok()
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .contentLength(Files.size(target))
      .body(new InputStreamResource(in));
  }

  @DeleteMapping("/store")
  public ResponseEntity<Void> delete(@RequestParam("path") String relPath) throws Exception {
    Path target = PathUtil.safeResolve(baseDir, relPath);
    if (Files.exists(target)) Files.delete(target);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/health")
  public ResponseEntity<String> health() { return ResponseEntity.ok("UP"); }
}
