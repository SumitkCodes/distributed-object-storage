package com.example.minis3.service;

import com.example.minis3.model.StorageNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import org.springframework.core.io.ByteArrayResource;

/**
 * Handles communication with individual storage nodes.
 * Manages file upload, download, and deletion operations.
 */
@Service
public class StorageNodeClient {
    
    private static final Logger logger = LoggerFactory.getLogger(StorageNodeClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    
    private final RestTemplate restTemplate;

    public StorageNodeClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Stores a file on the specified storage node.
     * 
     * @param node the target storage node
     * @param fileContent the file content as bytes
     * @param storagePath the storage path on the node
     * @throws RuntimeException if storage operation fails
     */
    public void store(StorageNode node, byte[] fileContent, String storagePath) {
        if (node == null || fileContent == null || storagePath == null) {
            throw new IllegalArgumentException("Node, file content, and storage path cannot be null");
        }

        String storeUrl = UriComponentsBuilder
            .fromHttpUrl(node.getBaseUrl() + "/store")
            .toUriString();

        try {
            logger.debug("Uploading file to node {} at path: {}", node.getName(), storagePath);
            
            // Create multipart request with file content and path
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Create a new ByteArrayResource for each upload to avoid stream issues
            ByteArrayResource fileResource = new ByteArrayResource(fileContent) {
                @Override
                public String getFilename() {
                    return "file";
                }
            };
            
            body.add("file", fileResource);
            body.add("path", storagePath);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                storeUrl, 
                HttpMethod.PUT, 
                requestEntity, 
                String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = String.format("Storage operation failed on node %s. Status: %s", 
                                              node.getName(), response.getStatusCode());
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            logger.debug("Successfully stored file on node {} at path: {}", node.getName(), storagePath);
            
        } catch (ResourceAccessException e) {
            String errorMsg = String.format("Connection failed to storage node %s: %s", node.getName(), e.getMessage());
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        } catch (RestClientException e) {
            String errorMsg = String.format("Storage operation failed on node %s: %s", node.getName(), e.getMessage());
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Fetches a file from the specified storage node.
     * 
     * @param node the source storage node
     * @param storagePath the storage path on the node
     * @return the file content as bytes
     * @throws RuntimeException if fetch operation fails
     */
    public byte[] fetch(StorageNode node, String storagePath) {
        if (node == null || storagePath == null) {
            throw new IllegalArgumentException("Node and storage path cannot be null");
        }

        String fetchUrl = UriComponentsBuilder
            .fromHttpUrl(node.getBaseUrl() + "/fetch")
            .queryParam("path", storagePath)
            .toUriString();

        try {
            logger.debug("Fetching file from node {} at path: {}", node.getName(), storagePath);
            
            ResponseEntity<byte[]> response = restTemplate.getForEntity(fetchUrl, byte[].class);
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                String errorMsg = String.format("Fetch failed on node %s. Status: %s", 
                                              node.getName(), response.getStatusCode());
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            logger.debug("Successfully fetched file from node {} at path: {}", node.getName(), storagePath);
            return response.getBody();
            
        } catch (ResourceAccessException e) {
            String errorMsg = String.format("Connection failed to storage node %s: %s", node.getName(), e.getMessage());
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        } catch (RestClientException e) {
            String errorMsg = String.format("Fetch operation failed on node %s: %s", node.getName(), e.getMessage());
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Deletes a file from the specified storage node.
     * 
     * @param node the target storage node
     * @param storagePath the storage path on the node
     * @throws RuntimeException if delete operation fails
     */
    public void delete(StorageNode node, String storagePath) {
        if (node == null || storagePath == null) {
            throw new IllegalArgumentException("Node and storage path cannot be null");
        }

        String deleteUrl = UriComponentsBuilder
            .fromHttpUrl(node.getBaseUrl() + "/store")
            .queryParam("path", storagePath)
            .toUriString();

        try {
            logger.debug("Deleting file from node {} at path: {}", node.getName(), storagePath);
            
            restTemplate.delete(deleteUrl);
            
            logger.debug("Successfully deleted file from node {} at path: {}", node.getName(), storagePath);
            
        } catch (ResourceAccessException e) {
            String errorMsg = String.format("Connection failed to storage node %s: %s", node.getName(), e.getMessage());
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        } catch (RestClientException e) {
            String errorMsg = String.format("Delete operation failed on node %s: %s", node.getName(), e.getMessage());
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }
}
