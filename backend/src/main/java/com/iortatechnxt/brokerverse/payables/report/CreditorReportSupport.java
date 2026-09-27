package com.iortatechnxt.brokerverse.payables.report;

import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.gl.GlReportSupport;
import com.iortatechnxt.brokerverse.subledger.service.AgeingService;
import com.iortatechnxt.brokerverse.subledger.service.AgeingSlots;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Parameters and ageing arithmetic shared by the creditors reports (FIN-AP-AGE-SUM, FIN-AP-AGE-DET,
 * FIN-AP-SUPOS): one engine parameterised by layout.
 *
 * <p>Rules: R-OPENITEM (balance as of date), R-AGE (slot by age on the chosen basis), R-ONAC
 * (payments and advances not allocated are shown separately in On A/c, with their sign, and are not
 * aged; Net = Σ buckets + On A/c), R-FX (base currency at the historical rate of the document).
 */
@Component
public class CreditorReportSupport {

  /** On account column key. */
  public static final String ON_ACCOUNT = "onAccount";

  /** Net value column key. */
  public static final String NET = "net";

  private static final String SLOT_PARAM = "slot";

  private final CreditorLedger ledger;
  private final OrganizationService organization;
  private final AgeingService ageing;

  /**
   * Creates the helper.
   *
   * @param ledger creditor read model
   * @param organization organization service
   * @param ageing sub-ledger ageing (company default slots)
   */
  public CreditorReportSupport(
      CreditorLedger ledger, OrganizationService organization, AgeingService ageing) {
    this.ledger = ledger;
    this.organization = organization;
    this.ageing = ageing;
  }

  /**
   * Common parameters: company, branch, as-of date, creditor type, party and main account ranges,
   * ageing basis, currency option and slots.
   *
   * @param withCurrencyOption include the Base / Foreign option
   * @return specs
   */
  public static List<ParameterSpec> parameters(boolean withCurrencyOption) {
    List<ParameterSpec> specs = new ArrayList<>();
    specs.add(GlReportSupport.companyParam());
    specs.add(GlReportSupport.branchParam());
    specs.add(GlReportSupport.asOfParam());
    specs.add(ReportParams.partyType());
    specs.addAll(
        ReportParams.range(ReportParams.PARTY_FROM, ReportParams.PARTY_TO, "Sub A/c (Party)"));
    specs.addAll(ReportParams.range(ReportParams.MAIN_FROM, ReportParams.MAIN_TO, "Main A/c"));
    specs.add(ReportParams.basis());
    if (withCurrencyOption) {
      specs.add(ReportParams.currencyMode());
    }
    specs.addAll(slotParameters());
    return specs;
  }

  /**
   * Ageing slot parameters {@code slot1..slot5}; all blank = the default slots.
   *
   * @return specs
   */
  public static List<ParameterSpec> slotParameters() {
    List<ParameterSpec> specs = new ArrayList<>();
    for (int i = 1; i <= AgeingSlots.MAX_SLOTS; i++) {
      specs.add(
          ParameterSpec.optional(
              SLOT_PARAM + i, "Ageing Slot " + i + " (days)", ParameterType.NUMBER));
    }
    return specs;
  }

  /**
   * Slots of a run: the slots entered, else the company default ({@code AGEING_BUCKETS}).
   *
   * @param p parameters
   * @return slots
   */
  public AgeingSlots slots(ReportParameters p) {
    return slots(p, ageing.defaultSlots());
  }

  /**
   * Slots of a run: the slots entered ({@code slot1..slot5}, blanks skipped), else the defaults.
   *
   * @param p parameters
   * @param defaults slots used when none is entered
   * @return slots
   */
  public static AgeingSlots slots(ReportParameters p, AgeingSlots defaults) {
    List<Integer> given = new ArrayList<>();
    for (int i = 1; i <= AgeingSlots.MAX_SLOTS; i++) {
      p.optionalDecimal(SLOT_PARAM + i).map(BigDecimal::intValue).ifPresent(given::add);
    }
    return given.isEmpty() ? defaults : AgeingSlots.of(given);
  }

  /**
   * Cell key of an ageing bucket.
   *
   * @param index bucket
   * @return key
   */
  public static String bucketKey(int index) {
    return "age" + index;
  }

  /**
   * Amount columns of every ageing bucket.
   *
   * @param slots slots
   * @return columns headed with the day ranges
   */
  public static List<ReportColumn> bucketColumns(AgeingSlots slots) {
    List<ReportColumn> cols = new ArrayList<>();
    for (int i = 0; i < slots.size(); i++) {
      cols.add(ReportColumn.amount(bucketKey(i), slots.label(i)));
    }
    return cols;
  }

  /**
   * Runs the creditor query for the parameters.
   *
   * @param p parameters
   * @return items
   */
  public List<CreditorItem> items(ReportParameters p) {
    return items(p, Set.of());
  }

  /**
   * Runs the creditor query, keeping some settled items.
   *
   * @param p parameters
   * @param alsoKeep item ids returned even with a zero balance
   * @return items
   */
  public List<CreditorItem> items(ReportParameters p, Set<Long> alsoKeep) {
    return ledger.openItems(
        new CreditorLedger.Query(
            p.longValue(GlReportSupport.COMPANY),
            p.date(GlReportSupport.AS_OF),
            CreditorLedger.partyTypes(ReportParams.text(p, ReportParams.PARTY_TYPE)),
            p.optionalLong(GlReportSupport.BRANCH).orElse(null),
            ReportParams.text(p, ReportParams.PARTY_FROM),
            ReportParams.text(p, ReportParams.PARTY_TO),
            ReportParams.text(p, ReportParams.MAIN_FROM),
            ReportParams.text(p, ReportParams.MAIN_TO)),
        alsoKeep);
  }

  /**
   * Whether amounts are reported in base currency.
   *
   * @param p parameters
   * @return true for BASE
   */
  public static boolean base(ReportParameters p) {
    return !ReportParams.FOREIGN.equals(ReportParams.text(p, ReportParams.CURRENCY_MODE));
  }

  /**
   * Whether items are aged by due date.
   *
   * @param p parameters
   * @return true for DUE_DATE
   */
  public static boolean byDueDate(ReportParameters p) {
    return !ReportParams.DOCUMENT_DATE.equals(ReportParams.text(p, ReportParams.BASIS));
  }

  /**
   * Company base currency.
   *
   * @param p parameters
   * @return currency code
   */
  public String baseCurrency(ReportParameters p) {
    return organization.getCompany(p.longValue(GlReportSupport.COMPANY)).getBaseCurrency();
  }

  /**
   * Adds an item to ageing cells: payables go to their age bucket, amounts on account to the On A/c
   * column; the net column receives both.
   *
   * @param cells row cells (bucket, on account and net keys are created as needed)
   * @param item item
   * @param slots slots
   * @param p parameters (as-of date, basis, currency option)
   */
  public static void accumulate(
      Map<String, Object> cells, CreditorItem item, AgeingSlots slots, ReportParameters p) {
    BigDecimal value = item.signed(base(p));
    String key =
        item.credit()
            ? bucketKey(slots.index(item.age(p.date(GlReportSupport.AS_OF), byDueDate(p))))
            : ON_ACCOUNT;
    cells.merge(key, value, (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
    cells.merge(NET, value, (a, b) -> ((BigDecimal) a).add((BigDecimal) b));
  }

  /**
   * Echo line describing the ageing.
   *
   * @param p parameters
   * @param slots slots
   * @return note
   */
  public static String note(ReportParameters p, AgeingSlots slots) {
    return "Aged on "
        + (byDueDate(p) ? "due date" : "document date")
        + " with slots "
        + slots.describe()
        + " days; "
        + (base(p) ? "amounts in base currency at document rates" : "amounts in document currency")
        + ". Payables are positive; unallocated payments and advances are shown in On A/c.";
  }
}
