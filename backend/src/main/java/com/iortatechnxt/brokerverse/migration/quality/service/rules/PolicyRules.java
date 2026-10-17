package com.iortatechnxt.brokerverse.migration.quality.service.rules;

import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.quality.service.FindingSink;
import com.iortatechnxt.brokerverse.migration.quality.service.ObjectRules;
import com.iortatechnxt.brokerverse.migration.quality.service.ValidationScope;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * In-force policy header rules (layouts P01 and P01S; DQ-014 to DQ-017): expiry after inception and
 * on or after the cut-over date, insurer shares of 100 percent with one lead, the client loaded,
 * and the product in the line of the header.
 */
@Component
public class PolicyRules implements ObjectRules {

  private static final String EXPIRY = "expiry_date";

  private static final String CLIENT_NO = "legacy_client_no";

  private final JdbcTemplate jdbc;

  /**
   * Creates the rules.
   *
   * @param jdbc JDBC (product lines of the catalogue)
   */
  public PolicyRules(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Set<String> layouts() {
    return Set.of("P01", "P01S");
  }

  @Override
  public void check(ValidationScope scope, FindingSink sink) {
    List<StageRow> headers = scope.rows("P01");
    LocalDate cutover = scope.parameters().cutoverDate();
    Set<String> clients = new HashSet<>();
    headers.forEach(r -> clients.add(r.getRawPayload().get(CLIENT_NO)));
    Set<String> loaded = scope.loaded("C01", clients);
    Map<String, String> lines = productLines();
    for (StageRow row : headers) {
      Map<String, String> v = row.getRawPayload();
      Optional<LocalDate> from = Values.date(v.get("inception_date"));
      Optional<LocalDate> to = Values.date(v.get(EXPIRY));
      if (from.isPresent() && to.isPresent() && !to.get().isAfter(from.get())) {
        sink.error(
            row, "DQ-014", EXPIRY, v.get(EXPIRY), "The expiry date is before the inception date");
      } else if (to.isPresent() && to.get().isBefore(cutover)) {
        sink.error(
            row,
            "DQ-014",
            EXPIRY,
            v.get(EXPIRY),
            "The policy expired before the cut-over date " + cutover + " and is not in force");
      }
      if (!loaded.contains(v.get(CLIENT_NO))) {
        sink.error(
            row,
            "DQ-016",
            CLIENT_NO,
            v.get(CLIENT_NO),
            "Client " + v.get(CLIENT_NO) + " is not loaded");
      }
      productInLine(row, scope.values(row), lines, sink);
    }
    Shares.check(scope.rows("P01S"), "legacy_policy_ref", "DQ-015", sink);
  }

  private static void productInLine(
      StageRow row, Map<String, String> mapped, Map<String, String> lines, FindingSink sink) {
    String product = mapped.get("risk_code");
    String line = mapped.get("line_code");
    String productLine = product == null ? null : lines.get(product);
    if (productLine != null && line != null && !productLine.equals(line)) {
      sink.error(
          row, "DQ-017", "risk_code", product, "Product " + product + " is not in line " + line);
    }
  }

  private Map<String, String> productLines() {
    Map<String, String> out = new HashMap<>();
    jdbc.query(
        "select code, line_code from cat_product",
        rs -> {
          out.put(rs.getString(1), rs.getString(2));
        });
    return out;
  }
}
