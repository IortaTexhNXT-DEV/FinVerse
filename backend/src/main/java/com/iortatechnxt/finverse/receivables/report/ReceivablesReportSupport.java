package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.receivables.domain.PayerType;
import com.iortatechnxt.finverse.receivables.service.ReceivablesQueries;
import com.iortatechnxt.finverse.receivables.service.ReceivablesQueries.ArItem;
import com.iortatechnxt.finverse.report.core.ParameterSpec;
import com.iortatechnxt.finverse.report.core.ParameterType;
import com.iortatechnxt.finverse.report.core.ReportColumn;
import com.iortatechnxt.finverse.report.core.ReportParameters;
import com.iortatechnxt.finverse.report.gl.GlReportSupport;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Parameters and calculations shared by the receivables reports (ageing rule R-AGE, on-account rule
 * R-ONAC, party and ledger selection).
 */
public final class ReceivablesReportSupport {

  /** Party range start parameter. */
  public static final String PARTY_FROM = "partyFrom";

  /** Party range end parameter. */
  public static final String PARTY_TO = "partyTo";

  /** Ledger (payer type) parameter. */
  public static final String LEDGER = "ledger";

  /** Ageing basis parameter. */
  public static final String ORDER_BY = "orderBy";

  /** Currency basis parameter. */
  public static final String CURRENCY_BASIS = "currencyBasis";

  /** Ageing slots parameter. */
  public static final String SLOTS = "ageingSlots";

  /** Due date basis option. */
  public static final String DUE_DATE = "DUE_DATE";

  /** Document date basis option. */
  public static final String DOCUMENT_DATE = "DOCUMENT_DATE";

  /** Base (local) currency option. */
  public static final String BASE = "BASE";

  /** Foreign (document) currency option. */
  public static final String FOREIGN = "FOREIGN";

  /** All debtor ledgers option. */
  public static final String ALL = "ALL";

  private ReceivablesReportSupport() {}

  /**
   * Party code range and ledger parameters.
   *
   * @return specs
   */
  public static List<ParameterSpec> partyParams() {
    List<String> ledgers = new ArrayList<>();
    ledgers.add(ALL);
    for (PayerType t : PayerType.values()) {
      if (t.hasParty()) {
        ledgers.add(t.name());
      }
    }
    return List.of(
        ParameterSpec.select(LEDGER, "Ledger", ledgers, ALL),
        ParameterSpec.optional(PARTY_FROM, "Party Code From", ParameterType.TEXT),
        ParameterSpec.optional(PARTY_TO, "Party Code To", ParameterType.TEXT));
  }

  /**
   * Ageing basis, currency basis and slots parameters.
   *
   * @return specs
   */
  public static List<ParameterSpec> ageingParams() {
    return List.of(
        ParameterSpec.select(
            ORDER_BY, "Order By (ageing basis)", List.of(DUE_DATE, DOCUMENT_DATE), DUE_DATE),
        ParameterSpec.select(CURRENCY_BASIS, "Currency", List.of(BASE, FOREIGN), BASE),
        ParameterSpec.optional(SLOTS, "Ageing Slots (days)", ParameterType.TEXT)
            .withDefault(AgeingSlots.DEFAULT));
  }

  /**
   * Parameters of the ageing reports: company, as-of date, party selection and ageing options.
   *
   * @return specs
   */
  public static List<ParameterSpec> ageingReportParams() {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(GlReportSupport.companyParam());
    params.add(GlReportSupport.asOfParam());
    params.addAll(partyParams());
    params.addAll(ageingParams());
    return params;
  }

  /**
   * Outstanding items (as of the as-of date) of the selected parties.
   *
   * @param queries read model
   * @param p parameters
   * @return items in party / document date order
   */
  public static List<ArItem> selectedItems(ReceivablesQueries queries, ReportParameters p) {
    return queries
        .itemsAsOf(p.longValue(GlReportSupport.COMPANY), p.date(GlReportSupport.AS_OF))
        .stream()
        .filter(partyFilter(p))
        .toList();
  }

  /**
   * Item filter for the party range and ledger parameters.
   *
   * @param p parameters
   * @return predicate
   */
  public static Predicate<ArItem> partyFilter(ReportParameters p) {
    String from = p.optionalText(PARTY_FROM).orElse("");
    String to = p.optionalText(PARTY_TO).orElse("￿");
    String ledger = p.optionalText(LEDGER).orElse(ALL);
    return i ->
        i.partyCode().compareTo(from) >= 0
            && i.partyCode().compareTo(to) <= 0
            && (ALL.equals(ledger) || ledger.equals(ledgerOf(i)));
  }

  /**
   * Ledger (payer type) of an item's party.
   *
   * @param i item
   * @return ledger name
   */
  public static String ledgerOf(ArItem i) {
    return PayerType.of(i.partyType()).name();
  }

  /**
   * Whether amounts are shown in the document currency.
   *
   * @param p parameters
   * @return true for FOREIGN
   */
  public static boolean foreign(ReportParameters p) {
    return FOREIGN.equals(p.optionalText(CURRENCY_BASIS).orElse(BASE));
  }

  /**
   * Ages outstanding items (R-AGE): DEBIT items go to the bucket of their age, CREDIT items
   * (unapplied receipts, credit notes) to On Account (R-ONAC).
   *
   * @param items items with a non-zero balance
   * @param asOf ageing date
   * @param p parameters (basis, currency basis, slots)
   * @return aged items
   */
  public static List<AgedItem> age(List<ArItem> items, LocalDate asOf, ReportParameters p) {
    return age(items, asOf, p, foreign(p));
  }

  /**
   * Ages outstanding items in an explicit currency basis.
   *
   * @param items items
   * @param asOf ageing date
   * @param p parameters (basis, slots)
   * @param foreign true for document currency amounts
   * @return aged items
   */
  public static List<AgedItem> age(
      List<ArItem> items, LocalDate asOf, ReportParameters p, boolean foreign) {
    boolean dueBasis = DUE_DATE.equals(p.optionalText(ORDER_BY).orElse(DUE_DATE));
    AgeingSlots slots = slots(p);
    return items.stream()
        .filter(i -> i.balance().signum() != 0)
        .map(
            i -> {
              long days = ChronoUnit.DAYS.between(dueBasis ? i.dueDate() : i.documentDate(), asOf);
              int bucket = i.direction() == ItemDirection.DEBIT ? slots.index(days) : -1;
              return new AgedItem(i, i.signedBalance(foreign), bucket, days);
            })
        .toList();
  }

  /**
   * Ageing slots of a run.
   *
   * @param p parameters
   * @return slots
   */
  public static AgeingSlots slots(ReportParameters p) {
    return AgeingSlots.parse(p.optionalText(SLOTS).orElse(AgeingSlots.DEFAULT));
  }

  /**
   * Cell key of an ageing bucket.
   *
   * @param bucket bucket index
   * @return key
   */
  public static String bucketKey(int bucket) {
    return "b" + bucket;
  }

  /**
   * Amount columns of the ageing buckets.
   *
   * @param slots slots
   * @return columns labelled with the day ranges
   */
  public static List<ReportColumn> bucketColumns(AgeingSlots slots) {
    List<String> labels = slots.labels();
    List<ReportColumn> columns = new ArrayList<>();
    for (int i = 0; i < labels.size(); i++) {
      columns.add(ReportColumn.amount(bucketKey(i), labels.get(i)));
    }
    return columns;
  }

  /**
   * Footnote describing the ageing basis.
   *
   * @param p parameters
   * @return note
   */
  public static String ageingNote(ReportParameters p) {
    String basis =
        DUE_DATE.equals(p.optionalText(ORDER_BY).orElse(DUE_DATE)) ? "due date" : "document date";
    return "Age = as-of date minus the "
        + basis
        + " (R-AGE); On Account = unapplied receipts and credits, not aged (R-ONAC); "
        + (foreign(p) ? "amounts in document currency" : "amounts in base currency");
  }

  /**
   * Party label used as group value.
   *
   * @param i item
   * @return "code - name"
   */
  public static String partyLabel(ArItem i) {
    return i.partyCode() + " - " + i.partyName();
  }

  /**
   * Item with its ageing.
   *
   * @param item item
   * @param amount signed balance in the report currency basis
   * @param bucket bucket index, -1 for On Account
   * @param days age in days
   */
  public record AgedItem(ArItem item, BigDecimal amount, int bucket, long days) {

    /**
     * Whether the item is shown as On Account (not aged).
     *
     * @return true for CREDIT items
     */
    public boolean onAccount() {
      return bucket < 0;
    }
  }
}
