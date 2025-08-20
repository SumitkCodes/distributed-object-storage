package com.example.minis3.service;

import com.example.minis3.model.StorageNode;
import com.example.minis3.repo.StorageNodeRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Manages file replication across multiple storage nodes.
 * Handles both upload replication and failover during downloads.
 */
@Service
public class ReplicationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReplicationService.class);
    
    private final PlacementService placementService;
    private final StorageNodeRepo nodeRepo;
    private final StorageNodeClient storageClient;

    public ReplicationService(PlacementService placementService, StorageNodeRepo nodeRepo, StorageNodeClient storageClient) {
        this.placementService = placementService;
        this.nodeRepo = nodeRepo;
        this.storageClient = storageClient;
    }

    /**
     * Replicates a file upload across multiple storage nodes.
     * 
     * @param bucketName the bucket containing the file
     * @param objectId the object identifier
     * @param version the version number
     * @param fileContent the file content
     * @param replicationFactor number of replicas to create
     * @return list of storage locations
     */
    @Transactional
    public List<Map<String, Object>> replicateUpload(String bucketName, Long objectId, Long version, 
                                                     byte[] fileContent, int replicationFactor) {
        if (bucketName == null || objectId == null || version == null || fileContent == null) {
            throw new IllegalArgumentException("All parameters must be non-null");
        }
        
        if (replicationFactor <= 0) {
            throw new IllegalArgumentException("Replication factor must be positive");
        }

        // Generate storage path for this version
        String storagePath = generateStoragePath(bucketName, objectId, version);
        
        // Select storage nodes for replication
        List<StorageNode> selectedNodes = placementService.selectReplicas(bucketName, 
                                                                        objectId.toString(), 
                                                                        replicationFactor);
        
        logger.info("Replicating file {} v{} to {} nodes", objectId, version, selectedNodes.size());
        
        // Perform sequential replication for better performance and reliability
        List<Map<String, Object>> storageLocations = new ArrayList<>();
        
        for (StorageNode node : selectedNodes) {
            try {
                storageClient.store(node, fileContent, storagePath);
                
                Map<String, Object> location = new HashMap<>();
                location.put("nodeId", node.getId());
                location.put("nodeName", node.getName());
                location.put("path", storagePath);
                location.put("status", "SUCCESS");
                
                storageLocations.add(location);
                logger.debug("Successfully replicated to node: {}", node.getName());
                
            } catch (Exception e) {
                logger.error("Failed to replicate to node {}: {}", node.getName(), e.getMessage());
                
                Map<String, Object> location = new HashMap<>();
                location.put("nodeId", node.getId());
                location.put("nodeName", node.getName());
                location.put("path", storagePath);
                location.put("status", "FAILED");
                location.put("error", e.getMessage());
                
                storageLocations.add(location);
            }
        }
        
        // Check if we have enough successful replications
        long successfulReplications = storageLocations.stream()
            .mapToLong(loc -> "SUCCESS".equals(loc.get("status")) ? 1 : 0)
            .sum();
        
        if (successfulReplications < replicationFactor) {
            logger.warn("Insufficient successful replications. Expected: {}, Got: {}", 
                       replicationFactor, successfulReplications);
        }
        
        return storageLocations;
    }

    /**
     * Fetches a file from storage nodes with failover support.
     * 
     * @param bucketName the bucket containing the file
     * @param objectId the object identifier
     * @param version the version number
     * @param locations the storage locations to try
     * @return ResponseEntity with file content or error
     */
    public ResponseEntity<byte[]> fetch(String bucketName, Long objectId, Long version, 
                                       List<Map<String, Object>> locations) {
        if (locations == null || locations.isEmpty()) {
            logger.error("No storage locations provided for fetch operation");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        // Try each location until one succeeds
        for (Map<String, Object> location : locations) {
            try {
                Long nodeId = extractNodeId(location);
                String storagePath = (String) location.get("path");
                
                if (nodeId == null || storagePath == null) {
                    logger.warn("Invalid location data: {}", location);
                    continue;
                }
                
                StorageNode node = nodeRepo.findById(nodeId).orElse(null);
                if (node == null) {
                    logger.warn("Storage node not found: {}", nodeId);
                    continue;
                }
                
                logger.debug("Attempting to fetch from node: {}", node.getName());
                
                byte[] fileContent = storageClient.fetch(node, storagePath);
                
                logger.info("Successfully fetched file {} v{} from node: {}", 
                           objectId, version, node.getName());
                
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(fileContent);
                    
            } catch (Exception e) {
                logger.warn("Failed to fetch from location {}: {}", location, e.getMessage());
                // Continue to next location
            }
        }
        
        logger.error("All storage locations failed for file {} v{}", objectId, version);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    /**
     * Generates a consistent storage path for a file version.
     * Note: Storage nodes seem to truncate bucket names to 4 characters.
     */
    private String generateStoragePath(String bucketName, Long objectId, Long version) {
        // Truncate bucket name to 4 characters to match storage node behavior
        String truncatedBucketName = bucketName.length() > 4 ? bucketName.substring(0, 4) : bucketName;
        return String.format("%s/%d/%d/blob", truncatedBucketName, objectId, version);
    }

    /**
     * Safely extracts node ID from location data.
     */
    private Long extractNodeId(Map<String, Object> location) {
        Object nodeIdObj = location.get("nodeId");
        if (nodeIdObj instanceof Number) {
            return ((Number) nodeIdObj).longValue();
        }
        return null;
    }
}
