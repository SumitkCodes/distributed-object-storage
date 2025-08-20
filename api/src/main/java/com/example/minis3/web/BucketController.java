package com.example.minis3.web;

import com.example.minis3.model.Bucket;
import com.example.minis3.repo.BucketRepo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST controller for managing storage buckets.
 * Provides endpoints for creating and listing buckets.
 */
@RestController
@RequestMapping("/buckets")
@Validated
public class BucketController {
    
    private static final Logger logger = LoggerFactory.getLogger(BucketController.class);
    private final BucketRepo bucketRepo;

    public BucketController(BucketRepo bucketRepo) {
        this.bucketRepo = bucketRepo;
    }

    /**
     * Creates a new storage bucket.
     * 
     * @param name the bucket name (validated)
     * @return the created bucket
     * @throws ResponseStatusException if bucket already exists or validation fails
     */
    @PostMapping
    public ResponseEntity<Bucket> createBucket(
            @RequestParam @NotBlank(message = "Bucket name is required")
            @Size(min = 3, max = 63, message = "Bucket name must be between 3 and 63 characters")
            @Pattern(regexp = "^[a-z0-9][a-z0-9.-]*[a-z0-9]$", 
                     message = "Bucket name must contain only lowercase letters, numbers, dots, and hyphens")
            String name) {
        
        logger.info("Creating bucket: {}", name);
        
        // Check if bucket already exists
        bucketRepo.findByName(name).ifPresent(existingBucket -> {
            String errorMsg = String.format("Bucket '%s' already exists", name);
            logger.warn(errorMsg);
            throw new ResponseStatusException(HttpStatus.CONFLICT, errorMsg);
        });
        
        try {
            Bucket newBucket = new Bucket(name);
            Bucket savedBucket = bucketRepo.save(newBucket);
            
            logger.info("Successfully created bucket: {} with ID: {}", name, savedBucket.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedBucket);
            
        } catch (Exception e) {
            String errorMsg = String.format("Failed to create bucket '%s': %s", name, e.getMessage());
            logger.error(errorMsg, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        }
    }

    /**
     * Lists all available buckets.
     * 
     * @return list of all buckets
     */
    @GetMapping
    public ResponseEntity<List<Bucket>> listBuckets() {
        try {
            List<Bucket> buckets = bucketRepo.findAll();
            logger.debug("Retrieved {} buckets", buckets.size());
            return ResponseEntity.ok(buckets);
            
        } catch (Exception e) {
            String errorMsg = "Failed to retrieve buckets: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        }
    }

    /**
     * Retrieves a specific bucket by name.
     * 
     * @param name the bucket name
     * @return the bucket if found
     * @throws ResponseStatusException if bucket not found
     */
    @GetMapping("/{name}")
    public ResponseEntity<Bucket> getBucket(@PathVariable String name) {
        logger.debug("Retrieving bucket: {}", name);
        
        Bucket bucket = bucketRepo.findByName(name)
            .orElseThrow(() -> {
                String errorMsg = String.format("Bucket '%s' not found", name);
                logger.warn(errorMsg);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, errorMsg);
            });
        
        return ResponseEntity.ok(bucket);
    }

    /**
     * Deletes a bucket (only if it's empty).
     * 
     * @param name the bucket name
     * @return no content on success
     * @throws ResponseStatusException if bucket not found or not empty
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteBucket(@PathVariable String name) {
        logger.info("Attempting to delete bucket: {}", name);
        
        Bucket bucket = bucketRepo.findByName(name)
            .orElseThrow(() -> {
                String errorMsg = String.format("Bucket '%s' not found", name);
                logger.warn(errorMsg);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, errorMsg);
            });
        
        // TODO: Check if bucket is empty before deletion
        // This would require checking for objects in the bucket
        
        try {
            bucketRepo.delete(bucket);
            logger.info("Successfully deleted bucket: {}", name);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            String errorMsg = String.format("Failed to delete bucket '%s': %s", name, e.getMessage());
            logger.error(errorMsg, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        }
    }
}
