package com.example.housing;

import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@Validated
@RequestMapping("/api/market")
public class MarketController {
    private static final Logger log = LoggerFactory.getLogger(MarketController.class);
    private final MarketService service;
    private final ObjectMapper json;
    private final URI modelPredictUri;
    public MarketController(MarketService service, @Value("${app.ml-url}") String mlUrl, ObjectMapper json) { this.service = service; this.modelPredictUri = URI.create(mlUrl + "/predict"); this.json = json; }

    @GetMapping("/summary")
    public Map<String,Object> summary(@RequestParam(required=false) @Min(0) Integer bedrooms,
            @RequestParam(required=false) @DecimalMin("0") Double minBathrooms,
            @RequestParam(required=false) Integer minYear, @RequestParam(required=false) Integer maxYear,
            @RequestParam(required=false) @Positive Double minSize, @RequestParam(required=false) @Positive Double maxSize) {
        if (minYear != null && maxYear != null && minYear > maxYear) throw new IllegalArgumentException("minYear must be <= maxYear");
        if (minSize != null && maxSize != null && minSize > maxSize) throw new IllegalArgumentException("minSize must be <= maxSize");
        return service.summary(service.filter(bedrooms,minBathrooms,minYear,maxYear,minSize,maxSize));
    }

    @PostMapping("/what-if")
    public ResponseEntity<?> whatIf(@RequestBody Map<String,Object> property) {
        HttpURLConnection connection = null;
        try {
            byte[] payload = json.writeValueAsBytes(Map.of("property", property));
            connection = (HttpURLConnection) modelPredictUri.toURL().openConnection(Proxy.NO_PROXY);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            connection.setRequestProperty("Accept", MediaType.APPLICATION_JSON_VALUE);
            connection.setDoOutput(true);
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(10000);
            connection.setFixedLengthStreamingMode(payload.length);
            try (var output = connection.getOutputStream()) { output.write(payload); }
            int status = connection.getResponseCode();
            var responseStream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String responseBody;
            if (responseStream == null) responseBody = "{}";
            else try (responseStream) { responseBody = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8); }
            if (status >= 400) {
                log.warn("Model API returned HTTP {}", status);
                return ResponseEntity.status(status).body(json.readValue(responseBody, Map.class));
            }
            return ResponseEntity.ok(json.readValue(responseBody, Map.class));
        }
        catch (JsonProcessingException e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("detail", "Could not serialize what-if request.")); }
        catch (Exception e) { log.warn("Model API call failed: {}", e.toString()); return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("detail", "Prediction service is unavailable.")); }
        finally { if (connection != null) connection.disconnect(); }
    }

    @GetMapping("/health") public Map<String,Object> health() { return Map.of("status", "ready"); }
}
