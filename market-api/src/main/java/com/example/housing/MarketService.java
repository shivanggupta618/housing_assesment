package com.example.housing;

import com.opencsv.CSVReader;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MarketService {
    @Value("${app.data-file}") private String dataFile;
    private List<PropertyRow> rows = List.of();

    @PostConstruct
    void load() throws Exception {
        try (CSVReader reader = new CSVReader(new FileReader(dataFile))) {
            List<String[]> records = reader.readAll();
            if (records.size() < 2 || !String.join(",", records.get(0)).equals(
                    "square_footage,bedrooms,bathrooms,year_built,lot_size,distance_to_city_center,school_rating,price"))
                throw new IllegalStateException("Housing CSV has an unexpected header or no data rows");
            rows = records.subList(1, records.size()).stream().map(r -> {
                if (r.length != 8) throw new IllegalStateException("Housing CSV has an invalid row");
                try { return new PropertyRow(Double.parseDouble(r[0]), Integer.parseInt(r[1]), Double.parseDouble(r[2]), Integer.parseInt(r[3]), Double.parseDouble(r[4]), Double.parseDouble(r[5]), Double.parseDouble(r[6]), Double.parseDouble(r[7])); }
                catch (RuntimeException e) { throw new IllegalStateException("Housing CSV contains non-numeric data", e); }
            }).toList();
        }
    }

    @Cacheable("marketRows")
    public List<PropertyRow> filter(Integer bedrooms, Double minBathrooms, Integer minYear, Integer maxYear, Double minSize, Double maxSize) {
        return rows.stream().filter(r -> bedrooms == null || r.bedrooms() == bedrooms)
                .filter(r -> minBathrooms == null || r.bathrooms() >= minBathrooms)
                .filter(r -> minYear == null || r.year_built() >= minYear)
                .filter(r -> maxYear == null || r.year_built() <= maxYear)
                .filter(r -> minSize == null || r.square_footage() >= minSize)
                .filter(r -> maxSize == null || r.square_footage() <= maxSize).toList();
    }

    public Map<String, Object> summary(List<PropertyRow> list) {
        if (list.isEmpty()) return Map.of("count", 0, "average", 0, "median", 0, "minimum", 0, "maximum", 0, "rows", list);
        double[] prices = list.stream().mapToDouble(PropertyRow::price).sorted().toArray();
        double avg = Arrays.stream(prices).average().orElse(0);
        double median = prices.length % 2 == 1 ? prices[prices.length / 2] : (prices[prices.length / 2 - 1] + prices[prices.length / 2]) / 2;
        return Map.of("count", prices.length, "average", avg, "median", median, "minimum", prices[0], "maximum", prices[prices.length - 1], "rows", list);
    }
}
