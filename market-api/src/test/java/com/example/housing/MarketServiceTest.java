package com.example.housing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MarketServiceTest {
    private MarketService service;

    @BeforeEach
    void loadCsv() throws Exception {
        service = new MarketService();
        ReflectionTestUtils.setField(service, "dataFile", Path.of("..", "training_data.csv").toAbsolutePath().normalize().toString());
        service.load();
    }

    @Test
    void aggregatesLabeledRowsFromTheSourceCsv() {
        var summary = service.summary(service.filter(null, null, null, null, null, null));
        assertThat(summary.get("count")).isEqualTo(21);
        assertThat((Double) summary.get("average")).isCloseTo(263333.3333, org.assertj.core.data.Offset.offset(0.01));
        assertThat(summary.get("median")).isEqualTo(240000.0);
    }

    @Test
    void filtersBedroomSegmentsWithoutChangingSourceRows() {
        var fourBedroomRows = service.filter(4, null, null, null, null, null);
        assertThat(fourBedroomRows).hasSize(5).allMatch(row -> row.bedrooms() == 4);
        assertThat(service.filter(null, null, null, null, null, null)).hasSize(21);
    }
}
