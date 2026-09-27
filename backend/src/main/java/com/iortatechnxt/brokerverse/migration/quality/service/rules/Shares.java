package com.iortatechnxt.brokerverse.migration.quality.service.rules;

import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.quality.service.FindingSink;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Check of insurer shares of a policy or an invoice: 100 percent and exactly one lead. */
final class Shares {

  private static final BigDecimal HUNDRED = new BigDecimal("100");

  private Shares() {}

  /**
   * Checks the share rows grouped by their parent key.
   *
   * @param rows share rows
   * @param parentColumn column of the parent key
   * @param rule rule code
   * @param sink findings
   */
  static void check(List<StageRow> rows, String parentColumn, String rule, FindingSink sink) {
    Map<String, List<StageRow>> byParent = new LinkedHashMap<>();
    for (StageRow r : rows) {
      byParent.computeIfAbsent(r.getRawPayload().get(parentColumn), k -> new ArrayList<>()).add(r);
    }
    for (List<StageRow> group : byParent.values()) {
      BigDecimal total = BigDecimal.ZERO;
      int leads = 0;
      for (StageRow r : group) {
        total =
            total.add(Values.decimal(r.getRawPayload().get("share_pct")).orElse(BigDecimal.ZERO));
        leads += Values.flag(r.getRawPayload().get("lead_flag")) ? 1 : 0;
      }
      if (total.compareTo(HUNDRED) != 0 || leads != 1) {
        StageRow first = group.get(0);
        sink.error(
            first,
            rule,
            "share_pct",
            total.toPlainString(),
            "Insurer shares add up to "
                + total.stripTrailingZeros().toPlainString()
                + " percent with "
                + leads
                + " lead insurer(s); they must add up to 100 with one lead");
      }
    }
  }
}
