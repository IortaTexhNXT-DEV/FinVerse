package com.iortatechnxt.brokerverse.migration.quality.service.rules;

import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.quality.service.FindingSink;
import com.iortatechnxt.brokerverse.migration.quality.service.ObjectRules;
import com.iortatechnxt.brokerverse.migration.quality.service.ValidationScope;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Trial balance and true-up rules (layouts G01, G03 and G03D; DQ-037, DQ-038, DQ-052, DQ-053,
 * DQ-055, DQ-056): a trial balance and each adjustment journal balance per branch and currency, a
 * line is a debit or a credit, an adjustment journal is dated in the last legacy fiscal year and
 * posted after the freeze, a line on a legacy control account has its open-item detail, and the
 * detailed items are loaded.
 */
@Component
public class GlRules implements ObjectRules {

  private static final String DEBIT_FC = "debit_fc";
  private static final String CREDIT_FC = "credit_fc";
  private static final String DEBIT = "debit_php";
  private static final String CREDIT = "credit_php";
  private static final String JOURNAL = "legacy_journal_no";
  private static final String CLEARING = "LGC-CLR";
  private static final String ITEM_REF = "item_ref";

  @Override
  public Set<String> layouts() {
    return Set.of("G01", "G03", "G03D");
  }

  @Override
  public void check(ValidationScope scope, FindingSink sink) {
    List<StageRow> tb = scope.rows("G01");
    tb.forEach(r -> oneSide(r, sink));
    balanced(tb, List.of("tb_version", "branch_code", "currency"), "DQ-037", sink);
    List<StageRow> journals = scope.rows("G03");
    balanced(journals, List.of(JOURNAL, "branch_code", "currency"), "DQ-052", sink);
    int year = scope.parameters().cutoverDate().getYear() - 1;
    LocalDateTime freeze = scope.parameters().freezeAt();
    Set<String> detailed = new HashSet<>();
    for (StageRow d : scope.rows("G03D")) {
      detailed.add(d.getRawPayload().get(JOURNAL) + "|" + d.getRawPayload().get("line_no"));
    }
    for (StageRow r : journals) {
      oneSide(r, sink);
      dated(r, year, freeze, sink);
      boolean control = CLEARING.equals(scope.values(r).get("legacy_account_code"));
      String line = r.getRawPayload().get(JOURNAL) + "|" + r.getRawPayload().get("line_no");
      if (control && !detailed.contains(line)) {
        sink.error(
            r,
            "DQ-055",
            "legacy_account_code",
            r.getRawPayload().get("legacy_account_code"),
            "The adjustment of a legacy control account has no open-item detail");
      }
    }
    items(scope, sink);
  }

  /** DQ-038: a line is a debit or a credit, not both. */
  private static void oneSide(StageRow r, FindingSink sink) {
    Map<String, String> v = r.getRawPayload();
    boolean debit = Values.amount(v.get(DEBIT_FC)).signum() != 0;
    boolean credit = Values.amount(v.get(CREDIT_FC)).signum() != 0;
    if (debit && credit) {
      sink.error(r, "DQ-038", DEBIT_FC, v.get(DEBIT_FC), "The line has a debit and a credit");
    }
  }

  /** Total debit = total credit (pesos) per group. */
  private static void balanced(
      List<StageRow> rows, List<String> keys, String rule, FindingSink sink) {
    Map<String, List<StageRow>> groups = new LinkedHashMap<>();
    for (StageRow r : rows) {
      List<String> parts = new ArrayList<>();
      keys.forEach(k -> parts.add(r.getRawPayload().get(k)));
      groups.computeIfAbsent(String.join(" ", parts), k -> new ArrayList<>()).add(r);
    }
    groups.forEach(
        (key, group) -> {
          BigDecimal net = BigDecimal.ZERO;
          for (StageRow r : group) {
            Map<String, String> v = r.getRawPayload();
            net = net.add(Values.amount(v.get(DEBIT))).subtract(Values.amount(v.get(CREDIT)));
          }
          if (net.signum() != 0) {
            sink.error(
                group.get(0),
                rule,
                DEBIT,
                net.toPlainString(),
                key + " is out of balance by " + net.toPlainString());
          }
        });
  }

  /** DQ-053: dated in the last legacy fiscal year and posted after the freeze. */
  private static void dated(StageRow r, int year, LocalDateTime freeze, FindingSink sink) {
    Map<String, String> v = r.getRawPayload();
    Optional<LocalDate> date = Values.date(v.get("legacy_journal_date"));
    Optional<LocalDateTime> posted = Values.stamp(v.get("posted_at"));
    boolean inYear = date.isPresent() && date.get().getYear() == year;
    boolean afterFreeze = posted.isPresent() && posted.get().isAfter(freeze);
    if (!inYear || !afterFreeze) {
      sink.error(
          r,
          "DQ-053",
          "legacy_journal_date",
          v.get("legacy_journal_date"),
          "Journal "
              + v.get(JOURNAL)
              + " must be dated in "
              + year
              + " and posted in legacy after the freeze");
    }
  }

  /** DQ-056: the detailed legacy invoices are loaded. */
  private static void items(ValidationScope scope, FindingSink sink) {
    List<StageRow> details = scope.rows("G03D");
    Set<String> refs = new HashSet<>();
    details.forEach(d -> refs.add(d.getRawPayload().get(ITEM_REF)));
    Set<String> loaded = scope.loaded("F01", refs);
    for (StageRow d : details) {
      Map<String, String> v = d.getRawPayload();
      if ("INVOICE".equalsIgnoreCase(v.get("item_kind")) && !loaded.contains(v.get(ITEM_REF))) {
        sink.error(
            d,
            "DQ-056",
            ITEM_REF,
            v.get(ITEM_REF),
            "Legacy invoice " + v.get(ITEM_REF) + " is not loaded");
      }
    }
  }
}
