package com.iortatechnxt.brokerverse.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Performance harness of the data migration (wave DM3; DATA_MIGRATION_DESIGN section 25): builds
 * synthetic client extracts of the size given by {@code -Dmig.perf.clients} (1,000,000 for the
 * dress-rehearsal sizing) and times intake, validation and load. It runs only when the property is
 * set, on the production-sized environment; the timings are logged for the runbook.
 */
@IntegrationTest
@EnabledIfSystemProperty(named = "mig.perf.clients", matches = "\\d+")
class MigrationPerformanceIT {

  private static final Logger LOG = LoggerFactory.getLogger(MigrationPerformanceIT.class);
  private static final String BASE = "/api/v1/migration";

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private ObjectMapper json;
  @Autowired private TestData data;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void clientsAreTakenThroughThePipelineWithinTheWindow() throws Exception {
    int size = Integer.parseInt(System.getProperty("mig.perf.clients"));
    long company = data.company().getId();
    MigrationTestSupport mig = new MigrationTestSupport(api, mvc, users, json, jdbc, company);
    mig.accepted("R01", "R02", "R03");
    mig.signMapping("C01");
    List<Map<String, String>> rows = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      Map<String, String> r = new HashMap<>();
      r.put("legacy_client_no", "PF" + i);
      r.put("client_type", "I");
      r.put("last_name", "Perf" + i);
      r.put("first_name", "Client");
      r.put("birth_date", LocalDate.of(1950, 1, 1).plusDays(i % 20_000L).toString());
      r.put("market_segment", "CBG");
      r.put("client_status", "A");
      r.put("created_date", "2020-01-01");
      r.put("last_updated", "2027-10-01 09:00:00");
      rows.add(r);
    }
    Instant start = Instant.now();
    JsonNode extract =
        mig.upload("C01", "C01", "C01_EBIX_20271231_99.csv", rows, List.of("legacy_client_no"));
    Instant staged = Instant.now();
    JsonNode batch = mig.load("C01", List.of(extract.get("extractNo").asText()));
    Instant loaded = Instant.now();
    LOG.info(
        "Migration performance: {} clients, intake {} s, validation and load {} s, batch {}",
        size,
        Duration.between(start, staged).toSeconds(),
        Duration.between(staged, loaded).toSeconds(),
        batch.get("batchNo").asText());
    assertThat(batch.get("counts").get("staged").asInt()).isEqualTo(size);
  }
}
