package com.example.storagenode.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtil {
  public static Path safeResolve(Path base, String unsafe) {
    Path p = base.resolve(unsafe).normalize();
    if (!p.startsWith(base)) {
      throw new IllegalArgumentException("Invalid path traversal attempt");
    }
    return p;
  }
}
