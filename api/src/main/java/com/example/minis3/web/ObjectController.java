package com.example.minis3.web;

import com.example.minis3.model.*;
import com.example.minis3.repo.*;
import com.example.minis3.service.ReplicationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * REST controller for managing file objects.
 * Handles upload, download, versioning, and listing operations.
 */
@RestController
@RequestMapping("/objects")
@Validated
public class ObjectController {
    
    private static final Logger logger = LoggerFactory.getLogger(ObjectController.class);
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int DEFAULT_REPLICATION_FACTOR = 2;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    private final BucketRepo bucketRepo;
    private final ObjectEntryRepo entryRepo;
    private final ObjectVersionRepo versionRepo;
    private final ReplicationService replicationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ObjectController(BucketRepo bucketRepo, ObjectEntryRepo entryRepo, 
                           ObjectVersionRepo versionRepo, ReplicationService replicationService) {
        this.bucketRepo = bucketRepo;
        this.entryRepo = entryRepo;
        this.versionRepo = versionRepo;
        this.replicationService = replicationService;
    }

    /**
     * Uploads a file to the specified bucket with automatic versioning.
     * 
     * @param bucketName the bucket name (validated)
     * @param objectKey the object key (validated)
     * @param file the uploaded file
     * @return upload result with metadata
     * @throws ResponseStatusException if upload fails
     */
    @PostMapping(path="/{bucketName}/{objectKey}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<Map<String, Object>> uploadFile(
            @PathVariable @NotBlank(message = "Bucket name is required")
            @Pattern(regexp = "^[a-z0-9][a-z0-9.-]*[a-z0-9]$", 
                     message = "Bucket name must contain only lowercase letters, numbers, dots, and hyphens")
            String bucketName,
            
            @PathVariable @NotBlank(message = "Object key is required")
            @Size(max = 1024, message = "Object key must not exceed 1024 characters")
            String objectKey,
            
            @RequestPart("file") MultipartFile file) {
        
        logger.info("Uploading file '{}' to bucket '{}'", objectKey, bucketName);
        
        try {
            // Validate file
            validateFile(file);
            
            // Find or create bucket
            Bucket bucket = bucketRepo.findByName(bucketName)
                .orElseThrow(() -> {
                    String errorMsg = String.format("Bucket '%s' not found", bucketName);
                    logger.warn(errorMsg);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, errorMsg);
                });

            // Validate object key
            if (StringUtils.isBlank(objectKey)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Object key cannot be empty");
            }

            // Find or create object entry
            ObjectEntry objectEntry = entryRepo.findByBucketIdAndObjectKey(bucket.getId(), objectKey)
                .orElseGet(() -> {
                    ObjectEntry newEntry = new ObjectEntry(bucket, objectKey);
                    return entryRepo.save(newEntry);
                });

            // Generate new version number
            long newVersion = objectEntry.getNextVersion();
            objectEntry.setNextVersion(newVersion + 1);
            entryRepo.save(objectEntry);

            // Read file content and calculate checksum
            byte[] fileContent = file.getBytes();
            String checksum = calculateChecksum(fileContent);

            logger.debug("File checksum: {} for version: {}", checksum, newVersion);

            // Replicate file across storage nodes
            List<Map<String, Object>> storageLocations = replicationService.replicateUpload(
                bucketName, objectEntry.getId(), newVersion, fileContent, DEFAULT_REPLICATION_FACTOR
            );

            // Create object version record
            ObjectVersion objectVersion = new ObjectVersion(objectEntry, newVersion);
            objectVersion.setSizeBytes((long) fileContent.length);
            objectVersion.setChecksum(checksum);
            
            try {
                objectVersion.setLocationsJson(objectMapper.writeValueAsString(storageLocations));
            } catch (Exception e) {
                String errorMsg = "Failed to serialize storage locations: " + e.getMessage();
                logger.error(errorMsg, e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
            }
            
            versionRepo.save(objectVersion);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("bucket", bucketName);
            response.put("key", objectKey);
            response.put("version", newVersion);
            response.put("checksum", checksum);
            response.put("sizeBytes", fileContent.length);
            response.put("replicationFactor", DEFAULT_REPLICATION_FACTOR);
            response.put("storageLocations", storageLocations.size());

            logger.info("Successfully uploaded file '{}' v{} to bucket '{}'", objectKey, newVersion, bucketName);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ResponseStatusException e) {
            throw e; // Re-throw validation errors
        } catch (Exception e) {
            String errorMsg = String.format("Failed to upload file '%s' to bucket '%s': %s", 
                                          objectKey, bucketName, e.getMessage());
            logger.error(errorMsg, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        }
    }

    /**
     * Downloads a file from the specified bucket.
     * 
     * @param bucketName the bucket name
     * @param objectKey the object key
     * @param version the specific version (optional, defaults to latest)
     * @return file content as response entity
     * @throws ResponseStatusException if download fails
     */
    @GetMapping("/{bucketName}/{objectKey}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable String bucketName,
            @PathVariable String objectKey,
            @RequestParam(required = false) Long version) {
        
        logger.debug("Downloading file '{}' from bucket '{}' (version: {})", 
                    objectKey, bucketName, version != null ? version : "latest");

        try {
            // Find bucket
            Bucket bucket = bucketRepo.findByName(bucketName)
                .orElseThrow(() -> {
                    String errorMsg = String.format("Bucket '%s' not found", bucketName);
                    logger.warn(errorMsg);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, errorMsg);
                });

            // Find object entry
            ObjectEntry objectEntry = entryRepo.findByBucketIdAndObjectKey(bucket.getId(), objectKey)
                .orElseThrow(() -> {
                    String errorMsg = String.format("Object '%s' not found in bucket '%s'", objectKey, bucketName);
                    logger.warn(errorMsg);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, errorMsg);
                });

            // Find object version
            ObjectVersion objectVersion;
            if (version == null) {
                // Get latest non-deleted version
                objectVersion = versionRepo.findTopByObjectEntryIdAndDeletedFalseOrderByVersionDesc(objectEntry.getId())
                    .orElseThrow(() -> {
                        String errorMsg = String.format("No versions available for object '%s'", objectKey);
                        logger.warn(errorMsg);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, errorMsg);
                    });
            } else {
                // Get specific version
                objectVersion = versionRepo.findByObjectEntryIdAndVersion(objectEntry.getId(), version)
                    .orElseThrow(() -> {
                        String errorMsg = String.format("Version %d not found for object '%s'", version, objectKey);
                        logger.warn(errorMsg);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, errorMsg);
                    });
                
                if (objectVersion.isDeleted()) {
                    String errorMsg = String.format("Version %d of object '%s' has been deleted", version, objectKey);
                    logger.warn(errorMsg);
                    throw new ResponseStatusException(HttpStatus.GONE, errorMsg);
                }
            }

            // Parse storage locations
            List<Map<String, Object>> storageLocations;
            try {
                storageLocations = objectMapper.readValue(objectVersion.getLocationsJson(), new TypeReference<>() {});
            } catch (Exception e) {
                String errorMsg = "Failed to parse storage locations: " + e.getMessage();
                logger.error(errorMsg, e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
            }

            // Fetch file content from storage nodes
            ResponseEntity<byte[]> fileResponse = replicationService.fetch(
                bucketName, objectEntry.getId(), objectVersion.getVersion(), storageLocations
            );

            if (fileResponse.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully downloaded file '{}' v{} from bucket '{}'", 
                           objectKey, objectVersion.getVersion(), bucketName);
                
                // Add metadata headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentLength(objectVersion.getSizeBytes());
                headers.set("X-Object-Checksum", objectVersion.getChecksum());
                headers.set("X-Object-Version", String.valueOf(objectVersion.getVersion()));
                headers.set("X-Object-Created", objectVersion.getCreatedAt().toString());
                
                return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileResponse.getBody());
            } else {
                String errorMsg = "Failed to retrieve file content from storage nodes";
                logger.error(errorMsg);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, errorMsg);
            }

        } catch (ResponseStatusException e) {
            throw e; // Re-throw validation errors
        } catch (Exception e) {
            String errorMsg = String.format("Failed to download file '%s' from bucket '%s': %s", 
                                          objectKey, bucketName, e.getMessage());
            logger.error(errorMsg, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        }
    }

    /**
     * Deletes a specific version of an object (soft delete).
     * 
     * @param bucketName the bucket name
     * @param objectKey the object key
     * @param version the version to delete
     * @return deletion result
     * @throws ResponseStatusException if deletion fails
     */
    @DeleteMapping("/{bucketName}/{objectKey}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteObjectVersion(
            @PathVariable String bucketName,
            @PathVariable String objectKey,
            @RequestParam Long version) {
        
        logger.info("Deleting version {} of object '{}' from bucket '{}'", version, objectKey, bucketName);

        try {
            // Find bucket
            Bucket bucket = bucketRepo.findByName(bucketName)
                .orElseThrow(() -> {
                    String errorMsg = String.format("Bucket '%s' not found", bucketName);
                    logger.warn(errorMsg);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, errorMsg);
                });

            // Find object entry
            ObjectEntry objectEntry = entryRepo.findByBucketIdAndObjectKey(bucket.getId(), objectKey)
                .orElseThrow(() -> {
                    String errorMsg = String.format("Object '%s' not found in bucket '%s'", objectKey, bucketName);
                    logger.warn(errorMsg);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, errorMsg);
                });

            // Find and mark version as deleted
            ObjectVersion objectVersion = versionRepo.findByObjectEntryIdAndVersion(objectEntry.getId(), version)
                .orElseThrow(() -> {
                    String errorMsg = String.format("Version %d not found for object '%s'", version, objectKey);
                    logger.warn(errorMsg);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, errorMsg);
                });

            objectVersion.setDeleted(true);
            versionRepo.save(objectVersion);

            Map<String, Object> response = new HashMap<>();
            response.put("deleted", true);
            response.put("bucket", bucketName);
            response.put("key", objectKey);
            response.put("version", version);
            response.put("deletedAt", new Date());

            logger.info("Successfully deleted version {} of object '{}' from bucket '{}'", 
                       version, objectKey, bucketName);
            return ResponseEntity.ok(response);

        } catch (ResponseStatusException e) {
            throw e; // Re-throw validation errors
        } catch (Exception e) {
            String errorMsg = String.format("Failed to delete version %d of object '%s': %s", 
                                          version, objectKey, e.getMessage());
            logger.error(errorMsg, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        }
    }

    /**
     * Lists objects in a bucket with optional prefix filtering.
     * 
     * @param bucketName the bucket name
     * @param prefix optional prefix filter
     * @return list of objects
     * @throws ResponseStatusException if listing fails
     */
    @GetMapping("/{bucketName}")
    public ResponseEntity<List<Map<String, Object>>> listObjects(
            @PathVariable String bucketName,
            @RequestParam(required = false) String prefix) {
        
        logger.debug("Listing objects in bucket '{}' with prefix: {}", bucketName, prefix);

        try {
            // Find bucket
            Bucket bucket = bucketRepo.findByName(bucketName)
                .orElseThrow(() -> {
                    String errorMsg = String.format("Bucket '%s' not found", bucketName);
                    logger.warn(errorMsg);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, errorMsg);
                });

            // Get object entries
            List<ObjectEntry> objectEntries;
            if (prefix == null || prefix.isEmpty()) {
                objectEntries = entryRepo.findByBucketId(bucket.getId());
            } else {
                objectEntries = entryRepo.findByBucketIdAndObjectKeyStartingWith(bucket.getId(), prefix);
            }

            // Build response
            List<Map<String, Object>> response = new ArrayList<>();
            for (ObjectEntry entry : objectEntries) {
                Map<String, Object> objectInfo = new HashMap<>();
                objectInfo.put("key", entry.getObjectKey());
                objectInfo.put("id", entry.getId());
                objectInfo.put("createdAt", entry.getCreatedAt());
                objectInfo.put("nextVersion", entry.getNextVersion());
                response.add(objectInfo);
            }

            logger.debug("Found {} objects in bucket '{}'", response.size(), bucketName);
            return ResponseEntity.ok(response);

        } catch (ResponseStatusException e) {
            throw e; // Re-throw validation errors
        } catch (Exception e) {
            String errorMsg = String.format("Failed to list objects in bucket '%s': %s", bucketName, e.getMessage());
            logger.error(errorMsg, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        }
    }

    /**
     * Validates uploaded file constraints.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            String errorMsg = String.format("File size %d bytes exceeds maximum allowed size of %d bytes", 
                                          file.getSize(), MAX_FILE_SIZE);
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, errorMsg);
        }

        // Additional file validation could be added here (e.g., file type, virus scanning)
    }

    /**
     * Calculates SHA-256 checksum of file content.
     */
    private String calculateChecksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(data);
            
            StringBuilder checksum = new StringBuilder();
            for (byte b : hashBytes) {
                checksum.append(String.format("%02x", b));
            }
            return checksum.toString();
            
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Checksum calculation failed", e);
        }
    }
}
