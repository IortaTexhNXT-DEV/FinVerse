package com.iortatechnxt.brokerverse.migration.quality.service.rules;

import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.mapping.domain.EntryAction;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMaps;
import com.iortatechnxt.brokerverse.migration.quality.service.FindingSink;
import com.iortatechnxt.brokerverse.migration.quality.service.ObjectRules;
import com.iortatechnxt.brokerverse.migration.quality.service.ValidationScope;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Reference data rules: every legacy list value of R01 is mapped in the code map of its list
 * ({@code LOV:<list>} or {@code MIS:<field>}, DQ-003); commission rates of an insurer and product
 * do not overlap (R07, DQ-043); the next number of a receipt series follows the last used number
 * within the series (R11, DQ-041).
 */
@Component
public class ReferenceRules implements ObjectRules {

  private static final String LOOKUP = "DQ-003";

  @Override
  public Set<String> layouts() {
    return Set.of("R01", "R07", "R11");
  }

  @Override
  public void check(ValidationScope scope, FindingSink sink) {
    lists(scope, sink);
    rates(scope, sink);
    series(scope, sink);
  }

  private static void lists(ValidationScope scope, FindingSink sink) {
    CodeMaps maps = scope.maps();
    for (StageRow row : scope.rows("R01")) {
      Map<String, String> v = row.getRawPayload();
      String list = Values.code(v.get("list_type"));
      String set = setOf(maps, list);
      if (set == null) {
        sink.error(row, LOOKUP, "list_type", list, "List " + list + " has no approved code map");
        continue;
      }
      String source = scope.source(row);
      Optional<CodeMaps.Resolution> r = maps.resolve(set, source, v.get("code"));
      if (r.isEmpty()) {
        sink.error(
            row,
            LOOKUP,
            "code",
            v.get("code"),
            "Code " + v.get("code") + " of " + set + " is not mapped");
      } else if (r.get().action() == EntryAction.REJECT) {
        sink.error(
            row,
            LOOKUP,
            "code",
            v.get("code"),
            "Code " + v.get("code") + " of " + set + " is rejected by the code map");
      } else {
        scope
            .values(row)
            .put("target_code", r.get().target() == null ? v.get("code") : r.get().target());
        scope.values(row).put("map_set", set);
        scope.values(row).put("map_action", r.get().action().name());
      }
    }
  }

  private static String setOf(CodeMaps maps, String list) {
    if (list == null) {
      return null;
    }
    if (maps.approved("LOV:" + list)) {
      return "LOV:" + list;
    }
    return maps.approved("MIS:" + list) ? "MIS:" + list : null;
  }

  private static void rates(ValidationScope scope, FindingSink sink) {
    Map<String, List<StageRow>> byKey = new HashMap<>();
    for (StageRow row : scope.rows("R07")) {
      Map<String, String> v = scope.values(row);
      byKey
          .computeIfAbsent(v.get("insurer_code") + "|" + v.get("risk_code"), k -> new ArrayList<>())
          .add(row);
    }
    for (List<StageRow> group : byKey.values()) {
      for (int i = 0; i < group.size(); i++) {
        for (int j = i + 1; j < group.size(); j++) {
          if (overlap(scope, group.get(i), group.get(j))) {
            Map<String, String> v = scope.values(group.get(j));
            sink.error(
                group.get(j),
                "DQ-043",
                "effective_from",
                v.get("effective_from"),
                "Rates of " + v.get("insurer_code") + " " + v.get("risk_code") + " overlap");
          }
        }
      }
    }
  }

  private static boolean overlap(ValidationScope scope, StageRow a, StageRow b) {
    LocalDate af = Values.date(scope.values(a).get("effective_from")).orElse(LocalDate.MIN);
    LocalDate at = Values.date(scope.values(a).get("effective_to")).orElse(LocalDate.MAX);
    LocalDate bf = Values.date(scope.values(b).get("effective_from")).orElse(LocalDate.MIN);
    LocalDate bt = Values.date(scope.values(b).get("effective_to")).orElse(LocalDate.MAX);
    return !af.isAfter(bt) && !bf.isAfter(at);
  }

  private static void series(ValidationScope scope, FindingSink sink) {
    for (StageRow row : scope.rows("R11")) {
      Map<String, String> v = row.getRawPayload();
      Optional<BigDecimal> from = Values.decimal(v.get("from_no"));
      Optional<BigDecimal> to = Values.decimal(v.get("to_no"));
      Optional<BigDecimal> last = Values.decimal(v.get("last_used_no"));
      Optional<BigDecimal> next = Values.decimal(v.get("next_no"));
      if (from.isEmpty() || to.isEmpty() || last.isEmpty() || next.isEmpty()) {
        continue;
      }
      boolean ok =
          next.get().compareTo(last.get().add(BigDecimal.ONE)) == 0
              && next.get().compareTo(from.get()) >= 0
              && next.get().compareTo(to.get()) <= 0;
      if (!ok) {
        sink.error(
            row,
            "DQ-041",
            "next_no",
            v.get("next_no"),
            "Next number " + v.get("next_no") + " is outside the series");
      }
    }
  }
}
