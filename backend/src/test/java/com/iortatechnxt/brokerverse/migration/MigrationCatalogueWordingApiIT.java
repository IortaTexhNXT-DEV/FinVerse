package com.iortatechnxt.brokerverse.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * The texts of the migration catalogue that the Migration Console shows - the rules (what is
 * checked and who fixes it) and the register ("Target in BIBS", rationale) - read in business
 * words: no record store, service or parameter of BIBS by its internal name, no raw status code.
 */
@IntegrationTest
class MigrationCatalogueWordingApiIT {

  /** Internal names: tables, services, parameter keys and short forms of the development. */
  private static final Pattern INTERNAL =
      Pattern.compile(
          "\\b(crm|xref|cat_[a-z_]+|acc_[a-z_]+|ops_[a-z_]+|csh_[a-z_]+|jnl_[a-z_]+|rem_[a-z_]+"
              + "|mig_[a-z_]+|org_[a-z_]+|lov_[a-z_]+|MIG_[A-Z_]+|dp_flag|APPROVED)\\b"
              + "|[A-Z][a-z]+(Service|Intake|Source|Accounts)\\b|\\.[a-z]+[A-Z][a-zA-Z]*\\b");

  @Autowired private Api api;

  @Test
  void theRulesAndTheRegisterReadInBusinessWords() throws Exception {
    List<String> found = new ArrayList<>();
    for (JsonNode r : read("/api/v1/migration/rules")) {
      check(found, r.get("code").asText(), r, "description", "message", "fixedBy");
    }
    for (JsonNode o : read("/api/v1/migration/objects")) {
      check(found, o.get("code").asText(), o, "name", "target", "conditionText", "rationale");
    }
    assertThat(found).isEmpty();
    JsonNode dq007 =
        read("/api/v1/migration/rules").findParents("code").stream()
            .filter(r -> "DQ-007".equals(r.get("code").asText()))
            .findFirst()
            .orElseThrow();
    assertThat(dq007.get("description").asText()).contains("client formats of BIBS");
  }

  private JsonNode read(String url) throws Exception {
    return api.read(api.doGet("miglead", url).andExpect(MockMvcResultMatchers.status().isOk()));
  }

  private static void check(List<String> found, String code, JsonNode node, String... fields) {
    for (String field : fields) {
      JsonNode value = node.get(field);
      if (value != null && INTERNAL.matcher(value.asText()).find()) {
        found.add(code + " " + field + ": " + value.asText());
      }
    }
  }
}
