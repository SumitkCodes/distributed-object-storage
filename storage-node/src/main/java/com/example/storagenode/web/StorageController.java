package com.example.storagenode.web;

import com.example.storagenode.util.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Map;

/**
 * REST controller for storage node operations.
 * Handles file storage, retrieval, and deletion with security validations.
 */
@RestController
@Validated
public class StorageController {

    private static final Logger logger = LoggerFactory.getLogger(StorageController.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "txt", "pdf", "doc", "docx", "jpg", "jpeg", "png", "gif", "zip", "rar", "bin"
    );

    private final Path baseDir;
    private final long maxFileSize;

    public StorageController(@Value("${storage.base-dir:./data}") String baseDir,
                           @Value("${storage.max-file-size:10485760}") String maxFileSizeStr) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
        
        // Parse the max file size string to bytes
        this.maxFileSize = parseFileSize(maxFileSizeStr);
        
        // Ensure base directory exists
        try {
            Files.createDirectories(this.baseDir);
            logger.info("Storage base directory initialized: {}", this.baseDir);
        } catch (IOException e) {
            logger.error("Failed to create base directory: {}", e.getMessage());
            throw new RuntimeException("Storage initialization failed", e);
        }
    }

    /**
     * Parses file size strings like "10MB", "1GB", etc. to bytes.
     */
    private long parseFileSize(String sizeStr) {
        if (sizeStr == null || sizeStr.trim().isEmpty()) {
            return 10 * 1024 * 1024; // Default 10MB
        }
        
        sizeStr = sizeStr.trim().toUpperCase();
        
        try {
            if (sizeStr.endsWith("KB")) {
                return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 2)) * 1024;
            } else if (sizeStr.endsWith("MB")) {
                return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 2)) * 1024 * 1024;
            } else if (sizeStr.endsWith("GB")) {
                return Long.parseLong(sizeStr.substring(0, sizeStr.length() - 2)) * 1024 * 1024 * 1024;
            } else {
                // Assume bytes if no unit specified
                return Long.parseLong(sizeStr);
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid file size format: {}, using default 10MB", sizeStr);
            return 10 * 1024 * 1024; // Default 10MB
        }
    }

    /**
     * Stores a file at the specified path.
     * 
     * @param file the uploaded file
     * @param relPath the relative storage path
     * @return success response
     * @throws ResponseStatusException if storage fails
     */
    @PutMapping(path="/store", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> storeFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart("path") String relPath) {
        
        logger.info("Storing file '{}' at path: {}", file.getOriginalFilename(), relPath);
        
        try {
            // Validate file
            validateFile(file);
            
            // Validate and resolve storage path
            Path targetPath = PathUtil.safeResolve(baseDir, relPath);
            
            // Ensure parent directories exist
            Files.createDirectories(targetPath.getParent());
            
            // Store the file
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            logger.info("Successfully stored file at: {}", targetPath);
            return ResponseEntity.ok("File stored successfully");
            
        } catch (IllegalArgumentException e) {
            String errorMsg = "Invalid storage path: " + e.getMessage();
            logger.warn(errorMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg);
        } catch (IOException e) {
            String errorMsg = "Failed to store file: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        } catch (Exception e) {
            String errorMsg = "Unexpected error during file storage: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        }
    }

    /**
     * Fetches a file from the specified path.
     * 
     * @param relPath the relative storage path
     * @return file content as stream
     * @throws ResponseStatusException if file not found or access fails
     */
    @GetMapping("/fetch")
    public ResponseEntity<InputStreamResource> fetchFile(@RequestParam("path") String relPath) {
        logger.debug("Fetching file from path: {}", relPath);
        
        try {
            // Validate and resolve storage path
            Path targetPath = PathUtil.safeResolve(baseDir, relPath);
            
            // Check if file exists and is a regular file
            if (!Files.exists(targetPath)) {
                logger.warn("File not found at path: {}", targetPath);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }
            
            if (!Files.isRegularFile(targetPath)) {
                logger.warn("Path is not a regular file: {}", targetPath);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path is not a file");
            }
            
            // Get file metadata
            long fileSize = Files.size(targetPath);
            String contentType = determineContentType(targetPath);
            
            // Create input stream
            InputStream inputStream = Files.newInputStream(targetPath, StandardOpenOption.READ);
            InputStreamResource resource = new InputStreamResource(inputStream);
            
            logger.debug("Successfully prepared file for download: {} ({} bytes)", targetPath, fileSize);
            
            // Build response with appropriate headers
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(fileSize)
                .header("X-File-Path", relPath)
                .header("X-File-Size", String.valueOf(fileSize))
                .body(resource);
                
        } catch (IllegalArgumentException e) {
            String errorMsg = "Invalid file path: " + e.getMessage();
            logger.warn(errorMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg);
        } catch (IOException e) {
            String errorMsg = "Failed to read file: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        } catch (ResponseStatusException e) {
            throw e; // Re-throw validation errors
        } catch (Exception e) {
            String errorMsg = "Unexpected error during file fetch: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        }
    }

    /**
     * Deletes a file from the specified path.
     * 
     * @param relPath the relative storage path
     * @return no content on success
     * @throws ResponseStatusException if deletion fails
     */
    @DeleteMapping("/store")
    public ResponseEntity<Void> deleteFile(@RequestParam("path") String relPath) {
        logger.info("Deleting file at path: {}", relPath);
        
        try {
            // Validate and resolve storage path
            Path targetPath = PathUtil.safeResolve(baseDir, relPath);
            
            // Check if file exists
            if (!Files.exists(targetPath)) {
                logger.warn("File not found for deletion: {}", targetPath);
                return ResponseEntity.noContent().build(); // Already deleted
            }
            
            // Delete the file
            Files.delete(targetPath);
            
            logger.info("Successfully deleted file at: {}", targetPath);
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            String errorMsg = "Invalid file path: " + e.getMessage();
            logger.warn(errorMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg);
        } catch (IOException e) {
            String errorMsg = "Failed to delete file: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        } catch (Exception e) {
            String errorMsg = "Unexpected error during file deletion: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        }
    }

    /**
     * Health check endpoint for the storage node.
     * 
     * @return health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        try {
            // Check if base directory is accessible
            if (!Files.isDirectory(baseDir) || !Files.isReadable(baseDir) || !Files.isWritable(baseDir)) {
                logger.error("Base directory is not accessible: {}", baseDir);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "DOWN", "reason", "Storage directory not accessible"));
            }
            
            // Get storage statistics
            long totalSpace = Files.getFileStore(baseDir).getTotalSpace();
            long usableSpace = Files.getFileStore(baseDir).getUsableSpace();
            long usedSpace = totalSpace - usableSpace;
            
            Map<String, Object> healthInfo = Map.of(
                "status", "UP",
                "baseDir", baseDir.toString(),
                "totalSpace", totalSpace,
                "usedSpace", usedSpace,
                "usableSpace", usableSpace,
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(healthInfo);
            
        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", "DOWN", "reason", e.getMessage()));
        }
    }

    /**
     * Validates uploaded file constraints.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File cannot be empty");
        }

        if (file.getSize() > maxFileSize) {
            String errorMsg = String.format("File size %d bytes exceeds maximum allowed size of %d bytes", 
                                          file.getSize(), maxFileSize);
            logger.warn(errorMsg);
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, errorMsg);
        }

        // Validate file extension if filename is available
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                String errorMsg = String.format("File extension '%s' is not allowed. Allowed: %s", 
                                              extension, String.join(", ", ALLOWED_EXTENSIONS));
                logger.warn(errorMsg);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg);
            }
        }
    }

    /**
     * Determines the content type based on file extension.
     */
    private String determineContentType(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        
        if (fileName.endsWith(".txt")) return "text/plain";
        if (fileName.endsWith(".pdf")) return "application/pdf";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".gif")) return "image/gif";
        if (fileName.endsWith(".zip")) return "application/zip";
        if (fileName.endsWith(".rar")) return "application/x-rar-compressed";
        if (fileName.endsWith(".bin")) return "application/octet-stream";
        
        // Default to binary
        return "application/octet-stream";
    }
}
