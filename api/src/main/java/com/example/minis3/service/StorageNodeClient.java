package com.example.minis3.service;

import com.example.minis3.model.StorageNode;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class StorageNodeClient {
  private final RestTemplate rest;

  public StorageNodeClient(RestTemplate rest) {
    this.rest = rest;
  }

  public void store(StorageNode node, ByteArrayResource file, String path) {
    String url = node.getBaseUrl() + "/store";
    MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
    map.add("file", file);
    map.add("path", path);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(map, headers);
    ResponseEntity<String> res = rest.exchange(url, HttpMethod.PUT, req, String.class);
    if (!res.getStatusCode().is2xxSuccessful()) {
      throw new RuntimeException("Failed to store on node " + node.getName());
    }
  }

  public byte[] fetch(StorageNode node, String path) {
    String url = UriComponentsBuilder.fromHttpUrl(node.getBaseUrl() + "/fetch")
      .queryParam("path", path).toUriString();
    ResponseEntity<byte[]> res = rest.getForEntity(url, byte[].class);
    if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
      throw new RuntimeException("Fetch failed on node " + node.getName());
    }
    return res.getBody();
  }

  public void delete(StorageNode node, String path) {
    String url = UriComponentsBuilder.fromHttpUrl(node.getBaseUrl() + "/store")
      .queryParam("path", path).toUriString();
    rest.delete(url);
  }
}
