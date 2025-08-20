package com.example.minis3.web;

import com.example.minis3.model.StorageNode;
import com.example.minis3.repo.StorageNodeRepo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing storage nodes.
 * Handles node registration and status monitoring.
 */
@RestController
@RequestMapping("/nodes")
@Validated
public class NodeController {
    
    private static final Logger logger = LoggerFactory.getLogger(NodeController.class);
    private final StorageNodeRepo nodeRepo;

    public NodeController(StorageNodeRepo nodeRepo) { 
        this.nodeRepo = nodeRepo; 
    }

    /**
     * Registers a new storage node or updates an existing one.
     * 
     * @param registrationData the node registration data
     * @return the registered/updated node
     * @throws ResponseStatusException if validation fails
     */
    @PostMapping("/register")
    public ResponseEntity<StorageNode> registerNode(@RequestBody @Valid NodeRegistrationRequest registrationData) {
        String nodeName = registrationData.getName();
        String baseUrl = registrationData.getBaseUrl();
        
        logger.info("Registering storage node: {} at {}", nodeName, baseUrl);
        
        try {
            // Check if node already exists
            StorageNode existingNode = nodeRepo.findByName(nodeName).orElse(null);
            
            if (existingNode != null) {
                // Update existing node
                logger.info("Updating existing node: {}", nodeName);
                existingNode.setBaseUrl(baseUrl);
                existingNode.setStatus("UP");
                existingNode.setLastHeartbeat(Instant.now());
                
                StorageNode updatedNode = nodeRepo.save(existingNode);
                
                logger.info("Successfully updated node: {}", nodeName);
                return ResponseEntity.ok(updatedNode);
                
            } else {
                // Create new node
                logger.info("Creating new node: {}", nodeName);
                StorageNode newNode = new StorageNode(nodeName, baseUrl);
                StorageNode savedNode = nodeRepo.save(newNode);
                
                logger.info("Successfully registered new node: {} with ID: {}", nodeName, savedNode.getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(savedNode);
            }
            
        } catch (Exception e) {
            String errorMsg = String.format("Failed to register node '%s': %s", nodeName, e.getMessage());
            logger.error(errorMsg, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        }
    }

    /**
     * Lists all registered storage nodes.
     * 
     * @return list of all storage nodes
     */
    @GetMapping
    public ResponseEntity<List<StorageNode>> listNodes() {
        try {
            List<StorageNode> nodes = nodeRepo.findAll();
            logger.debug("Retrieved {} storage nodes", nodes.size());
            return ResponseEntity.ok(nodes);
            
        } catch (Exception e) {
            String errorMsg = "Failed to retrieve storage nodes: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        }
    }

    /**
     * Updates the status of a storage node.
     * 
     * @param nodeName the name of the node
     * @param status the new status (UP, DOWN, MAINTENANCE)
     * @return the updated node
     * @throws ResponseStatusException if node not found or invalid status
     */
    @PutMapping("/{nodeName}/status")
    public ResponseEntity<StorageNode> updateNodeStatus(
            @PathVariable String nodeName,
            @RequestParam @Pattern(regexp = "^(UP|DOWN|MAINTENANCE)$", 
                                 message = "Status must be UP, DOWN, or MAINTENANCE")
            String status) {
        
        logger.info("Updating node {} status to: {}", nodeName, status);
        
        StorageNode node = nodeRepo.findByName(nodeName)
            .orElseThrow(() -> {
                String errorMsg = String.format("Storage node '%s' not found", nodeName);
                logger.warn(errorMsg);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, errorMsg);
            });
        
        try {
            node.setStatus(status);
            node.setLastHeartbeat(Instant.now());
            
            StorageNode updatedNode = nodeRepo.save(node);
            
            logger.info("Successfully updated node {} status to: {}", nodeName, status);
            return ResponseEntity.ok(updatedNode);
            
        } catch (Exception e) {
            String errorMsg = String.format("Failed to update node '%s' status: %s", nodeName, e.getMessage());
            logger.error(errorMsg, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        }
    }

    /**
     * Retrieves a specific storage node by name.
     * 
     * @param nodeName the name of the node
     * @return the storage node if found
     * @throws ResponseStatusException if node not found
     */
    @GetMapping("/{nodeName}")
    public ResponseEntity<StorageNode> getNode(@PathVariable String nodeName) {
        logger.debug("Retrieving storage node: {}", nodeName);
        
        StorageNode node = nodeRepo.findByName(nodeName)
            .orElseThrow(() -> {
                String errorMsg = String.format("Storage node '%s' not found", nodeName);
                logger.warn(errorMsg);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, errorMsg);
            });
        
        return ResponseEntity.ok(node);
    }

    /**
     * Request DTO for node registration.
     */
    public static class NodeRegistrationRequest {
        @NotBlank(message = "Node name is required")
        @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9._-]*[a-zA-Z0-9]$", 
                 message = "Node name must contain only letters, numbers, dots, underscores, and hyphens")
        private String name;
        
        @NotBlank(message = "Base URL is required")
        @Pattern(regexp = "^https?://[a-zA-Z0-9.-]+(:[0-9]+)?(/.*)?$", 
                 message = "Base URL must be a valid HTTP/HTTPS URL")
        private String baseUrl;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }
}
