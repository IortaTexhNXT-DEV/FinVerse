package com.iortatechnxt.finverse.reinsurance.report;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.party.domain.PartyType;
import com.iortatechnxt.finverse.party.service.PartyService;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportCategory;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportDefinition;
import com.iortatechnxt.finverse.report.core.ReportMetadata;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.core.ReportResult;
import com.iortatechnxt.finverse.report.core.TabularReportBuilder;
import com.iortatechnxt.finverse.security.domain.Permission;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import com.iortatechnxt.finverse.subledger.service.AgeingBucket;
import com.iortatechnxt.finverse.subledger.service.AgeingService;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * RI-BAL Reinsurer Balance / Ageing: outstanding open items of reinsurers and reinsurance brokers
 * at a date (all modules), per party: amounts due to the party (credit items), due from it (debit
 * items), the net balance (positive = due from the reinsurer) and the net aged by days past due.
 */
@Component
public class ReinsurerBalanceReport implements ReportDefinition {

  private static final String AS_OF = "asOfDate";
  private static final String REINSURER = "reinsurerCode";
  private static final String NET = "net";

  private final OpenItemService openItems;
  private final PartyService parties;

  /**
   * Creates the report.
   *
   * @param openItems party sub-ledger
   * @param parties reinsurers
   */
  public ReinsurerBalanceReport(OpenItemService openItems, PartyService parties) {
    this.openItems = openItems;
    this.parties = parties;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
        "RI-BAL",
        "Reinsurer Balance / Ageing",
        ReportCategory.REINSURANCE,
        "Balances due to and from reinsurers, aged by days past due",
        List.of(
            ParameterSpec.required(RiReportSupport.COMPANY, "Company", ParameterType.COMPANY),
            ParameterSpec.required(AS_OF, "As Of", ParameterType.DATE).withDefault("TODAY"),
            ParameterSpec.optional(REINSURER, "Reinsurer Code", ParameterType.TEXT)),
        Permission.REINSURANCE_VIEW);
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    Long companyId = p.longValue(RiReportSupport.COMPANY);
    LocalDate asOf = p.date(AS_OF);
    String only = p.optionalText(REINSURER).orElse(null);
    Map<String, Party> reinsurers =
        parties
            .search(companyId, EnumSet.of(PartyType.REINSURER, PartyType.RI_BROKER), null)
            .stream()
            .collect(Collectors.toMap(Party::getCode, Function.identity(), (a, b) -> a));
    Map<String, Map<String, Object>> rows = new LinkedHashMap<>();
    for (OpenItem item : openItems.outstanding(companyId, asOf)) {
      Party party = reinsurers.get(item.getPartyCode());
      if (party == null || only != null && !only.equals(party.getCode())) {
        continue;
      }
      Map<String, Object> row = rows.computeIfAbsent(party.getCode(), k -> emptyRow(party));
      BigDecimal signed = AgeingService.signedBaseOutstanding(item);
      add(row, item.getDirection() == ItemDirection.CREDIT ? "dueTo" : "dueFrom", signed.abs());
      add(row, NET, signed);
      long days = ChronoUnit.DAYS.between(item.getDueDate(), asOf);
      for (AgeingBucket b : AgeingService.DEFAULT_BUCKETS) {
        if (b.contains(days)) {
          add(row, key(b), signed);
        }
      }
    }
    List<ReportColumn> columns = new ArrayList<>();
    columns.add(ReportColumn.text("code", "Reinsurer"));
    columns.add(ReportColumn.text("name", "Name"));
    columns.add(ReportColumn.text("type", "Type"));
    columns.add(ReportColumn.amount("dueTo", "Due to Reinsurer"));
    columns.add(ReportColumn.amount("dueFrom", "Due from Reinsurer"));
    columns.add(ReportColumn.amount(NET, "Net (Dr + / Cr -)"));
    AgeingService.DEFAULT_BUCKETS.forEach(b -> columns.add(ReportColumn.amount(key(b), b.label())));
    return TabularReportBuilder.of(p)
        .columns(columns)
        .rows(new ArrayList<>(rows.values()))
        .presorted()
        .note("Base currency; net positive = due from the reinsurer, negative = due to it.")
        .build();
  }

  private static Map<String, Object> emptyRow(Party party) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("code", party.getCode());
    row.put("name", party.getName());
    row.put("type", party.getPartyType().name());
    row.put("dueTo", Money.zero());
    row.put("dueFrom", Money.zero());
    row.put(NET, Money.zero());
    AgeingService.DEFAULT_BUCKETS.forEach(b -> row.put(key(b), Money.zero()));
    return row;
  }

  private static String key(AgeingBucket b) {
    return "age" + b.fromDays();
  }

  private static void add(Map<String, Object> row, String key, BigDecimal value) {
    row.merge(key, value, (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
  }
}
