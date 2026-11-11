package com.example.housing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MarketControllerTest {
    @Test
    void whatIfForwardsAPropertyBodyToTheSharedModelApi() throws Exception {
        var captured = new AtomicReference<Map<String, Object>>();
        var mapper = new ObjectMapper();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/predict", exchange -> {
            captured.set(mapper.readValue(exchange.getRequestBody().readAllBytes(), new TypeReference<>() {}));
            byte[] response = "{\"predictions\":[{\"predicted_price\":260002.92}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) { output.write(response); }
        });
        server.start();
        try {
            var controller = new MarketController(null, "http://127.0.0.1:" + server.getAddress().getPort(), mapper);
            var property = Map.<String,Object>of(
                    "square_footage", 1650, "bedrooms", 3, "bathrooms", 2,
                    "year_built", 1999, "lot_size", 7100, "distance_to_city_center", 4.2,
                    "school_rating", 7.8);
            var response = controller.whatIf(property);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(captured.get()).containsKey("property");
            assertThat((Map<String,Object>) captured.get().get("property")).containsEntry("bedrooms", 3);
            assertThat((Map<String,Object>) response.getBody()).containsKey("predictions");
        } finally {
            server.stop(0);
        }
    }
}
