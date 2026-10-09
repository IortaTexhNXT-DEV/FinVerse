package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecordRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecordRepository.Tally;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordKind;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordStage;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Cashiering dashboard (FRS.CSH.01.03.01 to 01.03.04): the count and the amount per currency of
 * every item, counted when the dashboard opens or is refreshed: the AR / OR creation, cancellation
 * and reinstatement records For Posting per receipt type, the records Returned, the Unapplied
 * payments for disposition per type and the payment files waiting for automatic processing per file
 * type. Each item names the list it opens, already filtered.
 */
@Service
@Transactional(readOnly = true)
public class CashieringDashboardService {

  private static final String COMMISSION_TYPE = "COMMISSION";

  /** AR types of the dashboard. */
  static final List<String> AR_TYPES =
      List.of("PREMIUM", "REFUND", "OTHER_EXPENSES", "AR_INSURANCE");

  /** OR types of the dashboard. */
  static final List<String> OR_TYPES =
      List.of("SERVICE_FEE", "PROFIT_SHARE", COMMISSION_TYPE, "INCENTIVE", "OTHERS");

  private static final Map<String, String> TYPE_LABELS =
      Map.ofEntries(
          Map.entry("PREMIUM", "Premium"),
          Map.entry("REFUND", "Non-Premium - Refund"),
          Map.entry("OTHER_EXPENSES", "Non-Premium - Other Expenses"),
          Map.entry("AR_INSURANCE", "Non-Premium - AR Insurance"),
          Map.entry("SERVICE_FEE", "Service Fee"),
          Map.entry("PROFIT_SHARE", "Insurance Profit Share"),
          Map.entry(COMMISSION_TYPE, "Commissions"),
          Map.entry("INCENTIVE", "Incentives"),
          Map.entry("OTHERS", "Others"));

  private static final String POSTING = "/cashiering/posting?kind=";

  private static final String UNAPPLIED =
      "select case"
          + "   when u.origin in ('NO_MATCH', 'CANCELLED_REFERENCE') then 'UNBOOKED'"
          + "   when u.origin = 'EXCESS' then 'EXCESS'"
          + "   when u.origin = 'PREBOOKED' then 'PREBOOKED'"
          + "   when u.origin = 'AR_INSURER_REFUND' then 'REFUND_INSURER'"
          + "   when u.origin in ('AP_UNAPPLIED_COMMISSION', 'COMMISSION_RECEIVABLE') then 'COMMISSION'"
          + "   when exists (select 1 from csh_disposition d where d.unapplied_id = u.id"
          + "     and d.action = 'REFUND' and d.status in ('FOR_APPROVAL', 'IN_PROCESS')) then 'REFUND_CLIENT'"
          + "   else 'UNAPPLIED' end as type, u.currency, count(*) as n, sum(u.balance) as amount"
          + " from csh_unapplied u where u.company_id = ? and u.balance > 0 and u.stage <> 'CLOSED'"
          + " group by 1, 2";

  private static final String PREBOOKED =
      "select currency, count(*) as n, sum(amount) as amount from csh_prebooked"
          + " where company_id = ? and status = 'OPEN' group by currency";

  private static final String FILES =
      "select handler_code, count(*) as n from bulk_job where company_id = ?"
          + " and handler_code in ('PAY_BILLS', 'PAY_CLPC', 'PAY_TRADE', 'PAY_DIRECT_CREDIT', 'PAY_PDC')"
          + " and status = 'VALIDATED' group by handler_code";

  private static final List<String[]> UNAPPLIED_ITEMS =
      List.of(
          new String[] {"PREBOOKED", "Pre-Booked"},
          new String[] {"EXCESS", "Excess"},
          new String[] {"REFUND_CLIENT", "Refund to Client"},
          new String[] {"REFUND_INSURER", "Refund to Insurer"},
          new String[] {"UNBOOKED", "Unbooked/Unmatched"},
          new String[] {"UNAPPLIED", "Unapplied"},
          new String[] {COMMISSION_TYPE, "Unapplied Commission"});

  private static final List<String[]> FILE_ITEMS =
      List.of(
          new String[] {"PAY_PDC", "PDC"},
          new String[] {"PAY_BILLS", "Bills Payment"},
          new String[] {"PAY_CLPC", "CLPC"},
          new String[] {"PAY_TRADE", "Trade"},
          new String[] {"PAY_DIRECT_CREDIT", "Direct Credit"});

  private final ReceiptRecordRepository records;
  private final JdbcTemplate jdbc;

  /**
   * Creates the dashboard.
   *
   * @param records records
   * @param jdbc unapplied payments and payment files
   */
  public CashieringDashboardService(ReceiptRecordRepository records, JdbcTemplate jdbc) {
    this.records = records;
    this.jdbc = jdbc;
  }

  /**
   * The dashboard of a company.
   *
   * @param companyId company
   * @return groups with their items
   */
  public List<Group> dashboard(Long companyId) {
    List<Tally> tallies =
        records.tally(companyId, EnumSet.of(RecordStage.FOR_POSTING, RecordStage.RETURNED));
    List<Group> groups = new ArrayList<>();
    groups.add(forPosting("ISSUANCE", "AR/OR for Posting", RecordKind.CREATION, tallies));
    groups.add(
        forPosting(
            "CANCELLATION", "Cancellation (AR/OR) for Posting", RecordKind.CANCELLATION, tallies));
    groups.add(
        forPosting(
            "REINSTATEMENT",
            "Reinstatement (AR/OR) for Posting",
            RecordKind.REINSTATEMENT,
            tallies));
    groups.add(returned(tallies));
    groups.add(unapplied(companyId));
    groups.add(files(companyId));
    return groups;
  }

  private Group forPosting(String code, String label, RecordKind kind, List<Tally> tallies) {
    List<Item> items = new ArrayList<>();
    for (ReceiptKind receipt : ReceiptKind.values()) {
      for (String type : receipt == ReceiptKind.AR ? AR_TYPES : OR_TYPES) {
        Item item =
            item(
                code + "_" + receipt + "_" + type,
                receipt + " - " + TYPE_LABELS.get(type),
                POSTING + kind + "&receiptKind=" + receipt + "&type=" + type + "&stage=FOR_POSTING",
                tallies.stream()
                    .filter(t -> t.kind() == kind && t.receiptKind() == receipt)
                    .filter(t -> t.stage() == RecordStage.FOR_POSTING)
                    .filter(t -> type.equals(dashboardType(receipt, t.receiptType())))
                    .toList());
        items.add(item);
      }
    }
    return new Group(code, label, items);
  }

  private Group returned(List<Tally> tallies) {
    List<Item> items = new ArrayList<>();
    Map<RecordKind, String> names =
        Map.of(
            RecordKind.CREATION, "Issuance",
            RecordKind.CANCELLATION, "Cancellation",
            RecordKind.REINSTATEMENT, "Reinstatement");
    for (ReceiptKind receipt : ReceiptKind.values()) {
      for (RecordKind kind : RecordKind.values()) {
        items.add(
            item(
                "RETURNED_" + receipt + "_" + kind,
                receipt + " - " + names.get(kind),
                POSTING + kind + "&receiptKind=" + receipt + "&stage=RETURNED",
                tallies.stream()
                    .filter(t -> t.kind() == kind && t.receiptKind() == receipt)
                    .filter(t -> t.stage() == RecordStage.RETURNED)
                    .toList()));
      }
    }
    return new Group("RETURNED", "Returned List", items);
  }

  /**
   * The list an unapplied payment item opens: the list of the unapplied payments of its type, or
   * the disposition workbench for the refunds to clients.
   */
  private static String unappliedLink(String key) {
    return switch (key) {
      case "REFUND_CLIENT" -> "/cashiering/unapplied";
      case "REFUND_INSURER" -> "/cashiering/unapplied-inquiry?type=AR_INSURER_REFUND";
      case COMMISSION_TYPE -> "/cashiering/unapplied-inquiry?type=AP_UNAPPLIED_COMMISSION";
      default -> "/cashiering/unapplied-inquiry?type=" + key;
    };
  }

  private Group unapplied(Long companyId) {
    Map<String, Item> byType = new LinkedHashMap<>();
    for (String[] item : UNAPPLIED_ITEMS) {
      byType.put(
          item[0],
          new Item("UNAPPLIED_" + item[0], item[1], 0, new TreeMap<>(), unappliedLink(item[0])));
    }
    jdbc.query(
        UNAPPLIED,
        rs -> {
          Item item = byType.get(rs.getString("type"));
          byType.put(
              item.code().substring("UNAPPLIED_".length()),
              item.plus(rs.getLong("n"), rs.getString("currency"), rs.getBigDecimal("amount")));
        },
        companyId);
    jdbc.query(
        PREBOOKED,
        rs -> {
          Item item = byType.get("PREBOOKED");
          byType.put(
              "PREBOOKED",
              item.plus(rs.getLong("n"), rs.getString("currency"), rs.getBigDecimal("amount")));
        },
        companyId);
    return new Group(
        "UNAPPLIED", "Unapplied Payments for Disposition", List.copyOf(byType.values()));
  }

  private Group files(Long companyId) {
    Map<String, Long> counts = new LinkedHashMap<>();
    jdbc.query(
        FILES,
        rs -> {
          counts.put(rs.getString("handler_code"), rs.getLong("n"));
        },
        companyId);
    List<Item> items = new ArrayList<>();
    for (String[] file : FILE_ITEMS) {
      items.add(
          new Item(
              "FILES_" + file[0],
              file[1],
              counts.getOrDefault(file[0], 0L),
              Map.of(),
              "/cashiering/uploads?type=" + file[0]));
    }
    return new Group("FILES", "For Auto Payment Processing", items);
  }

  /**
   * The dashboard type of a record's AR class or OR type.
   *
   * @param receipt AR or OR
   * @param type AR class or OR type
   * @return dashboard type
   */
  static String dashboardType(ReceiptKind receipt, String type) {
    if (receipt == ReceiptKind.OR) {
      return OR_TYPES.contains(type) ? type : "OTHERS";
    }
    return Set.copyOf(AR_TYPES).contains(type) ? type : "PREMIUM";
  }

  private static Item item(String code, String label, String link, List<Tally> tallies) {
    Item item = new Item(code, label, 0, new TreeMap<>(), link);
    for (Tally t : tallies) {
      item = item.plus(t.count(), t.currency(), t.amount());
    }
    return item;
  }

  /**
   * A group of the dashboard.
   *
   * @param code code
   * @param label label
   * @param items items
   */
  public record Group(String code, String label, List<Item> items) {

    /** Defensive copy. */
    public Group {
      items = List.copyOf(items);
    }
  }

  /**
   * An item of the dashboard.
   *
   * @param code code
   * @param label label
   * @param count records
   * @param amounts amount per currency
   * @param link list the item opens
   */
  public record Item(
      String code, String label, long count, Map<String, BigDecimal> amounts, String link) {

    /** Defensive copy. */
    public Item {
      amounts = Map.copyOf(amounts);
    }

    Item plus(long n, String currency, BigDecimal amount) {
      Map<String, BigDecimal> sum = new TreeMap<>(amounts);
      if (currency != null && amount != null) {
        sum.merge(currency, amount, BigDecimal::add);
      }
      return new Item(code, label, count + n, sum, link);
    }
  }
}
