package com.example.minis3.service;

import com.example.minis3.model.StorageNode;
import com.example.minis3.repo.StorageNodeRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Handles intelligent placement of file replicas across storage nodes.
 * Uses consistent hashing to ensure even distribution and predictable placement.
 */
@Service
public class PlacementService {
    
    private static final Logger logger = LoggerFactory.getLogger(PlacementService.class);
    private static final String HASH_ALGORITHM = "SHA-256";
    
    private final StorageNodeRepo nodeRepo;

    public PlacementService(StorageNodeRepo nodeRepo) {
        this.nodeRepo = nodeRepo;
    }

    /**
     * Selects the best storage nodes for file replication based on consistent hashing.
     * 
     * @param bucketName the bucket containing the file
     * @param objectKey the object key (filename)
     * @param replicationFactor number of replicas to create
     * @return list of selected storage nodes
     * @throws IllegalStateException if insufficient nodes are available
     */
    public List<StorageNode> selectReplicas(String bucketName, String objectKey, int replicationFactor) {
        if (bucketName == null || objectKey == null) {
            throw new IllegalArgumentException("Bucket name and object key cannot be null");
        }
        
        if (replicationFactor <= 0) {
            throw new IllegalArgumentException("Replication factor must be positive");
        }

        List<StorageNode> availableNodes = getAvailableNodes();
        
        if (availableNodes.size() < replicationFactor) {
            String errorMsg = String.format("Insufficient available nodes. Required: %d, Available: %d", 
                                          replicationFactor, availableNodes.size());
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // Create a composite key for consistent hashing
        String compositeKey = bucketName + "/" + objectKey;
        
        // Score nodes based on consistent hashing
        List<Map.Entry<StorageNode, Double>> scoredNodes = new ArrayList<>();
        for (StorageNode node : availableNodes) {
            double score = calculateNodeScore(compositeKey, node.getName());
            scoredNodes.add(Map.entry(node, score));
        }
        
        // Sort by score (descending) and select top nodes
        scoredNodes.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        List<StorageNode> selectedNodes = new ArrayList<>();
        for (int i = 0; i < replicationFactor; i++) {
            selectedNodes.add(scoredNodes.get(i).getKey());
        }
        
        logger.debug("Selected nodes for {}: {}", compositeKey, 
                    selectedNodes.stream().map(StorageNode::getName).toList());
        
        return selectedNodes;
    }

    /**
     * Gets available (UP) storage nodes.
     */
    private List<StorageNode> getAvailableNodes() {
        List<StorageNode> nodes = nodeRepo.findByStatusIgnoreCase("UP");
        logger.debug("Found {} available storage nodes", nodes.size());
        return nodes;
    }

    /**
     * Calculates a deterministic score for a node based on the object key.
     * Uses SHA-256 hashing for consistent distribution.
     */
    private double calculateNodeScore(String compositeKey, String nodeName) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            String input = compositeKey + "@" + nodeName;
            byte[] hashBytes = digest.digest(input.getBytes());
            
            // Use first 8 bytes for deterministic scoring
            ByteBuffer buffer = ByteBuffer.wrap(hashBytes, 0, 8);
            long hashValue = buffer.getLong();
            
            // Apply bit manipulation for better distribution
            long processedValue = hashValue ^ (hashValue >>> 1);
            
            // Normalize to [0, 1) range
            return (processedValue >>> 11) * (1.0 / (1L << 53));
            
        } catch (NoSuchAlgorithmException e) {
            logger.warn("SHA-256 algorithm not available, using fallback scoring for node: {}", nodeName);
            // Fallback to a deterministic alternative
            return Math.abs((compositeKey + nodeName).hashCode()) / (double) Integer.MAX_VALUE;
        }
    }
}
