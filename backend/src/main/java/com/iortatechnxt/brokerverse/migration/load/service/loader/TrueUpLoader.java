package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.load.service.LoadContext;
import com.iortatechnxt.brokerverse.migration.load.service.LoadOutcome;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import com.iortatechnxt.brokerverse.migration.trueup.domain.MigTrueup;
import com.iortatechnxt.brokerverse.migration.trueup.domain.MigTrueupRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceRepository;
import com.iortatechnxt.brokerverse.opsledger.service.LegacyInvoiceIntake;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Loader of a FY2027 true-up (object G03 with the open-item detail G03D; DATA_MIGRATION_DESIGN
 * 17.7). Only an approved true-up is posted: per branch and currency, the opening-balance
 * adjustment journal (type OPENING, source MIGRATION, reference MIG-TU-&lt;n&gt;) with the P&amp;L
 * lines on retained earnings and the legacy control accounts on Migration Clearing; for each
 * open-item detail line the legacy invoice's component is adjusted ({@code LEGACY_ADJUSTED} or
 * {@code LEGACY_WRITTEN_OFF}) with its event {@code MIG_LEGACY_POSITION_TRUEUP}. An adjustment that
 * would take an open item below zero is refused (it usually means the item moved in BIBS since
 * go-live). A posted true-up is never rolled back: an error is corrected by the next true-up.
 */
@Component
public class TrueUpLoader implements MigrationLoader {

  private static final String DETAIL = "G03D";
  private static final String BRANCH = "branch_code";
  private static final String CURRENCY = "currency";
  private static final String UNREALIZED = "UNREALIZED";
  private static final String DEFERRED_VAT = "DEFERRED_VAT";

  private final OpeningJournals journals;
  private final MigTrueupRepository trueups;
  private final LegacyInvoiceIntake intake;
  private final LegacyInvoicePosting posting;
  private final OpsInvoiceRepository invoices;
  private final BranchRepository branches;
  private final MigrationParameters parameters;

  /**
   * Creates the loader.
   *
   * @param journals opening journals
   * @param trueups true-ups
   * @param intake legacy invoice intake (opening adjustments)
   * @param posting legacy invoice events
   * @param invoices ledger invoices
   * @param branches branches
   * @param parameters migration parameters
   */
  public TrueUpLoader(
      OpeningJournals journals,
      MigTrueupRepository trueups,
      LegacyInvoiceIntake intake,
      LegacyInvoicePosting posting,
      OpsInvoiceRepository invoices,
      BranchRepository branches,
      MigrationParameters parameters) {
    this.journals = journals;
    this.trueups = trueups;
    this.intake = intake;
    this.posting = posting;
    this.invoices = invoices;
    this.branches = branches;
    this.parameters = parameters;
  }

  @Override
  public String objectCode() {
    return "G03";
  }

  @Override
  public List<String> childLayouts() {
    return List.of(DETAIL);
  }

  @Override
  public List<LoadUnit> group(List<LoadUnit> units) {
    return JournalGroups.group(units, u -> u.value(BRANCH) + "|" + u.value(CURRENCY));
  }

  @Override
  public String partitionKey(LoadUnit unit) {
    return unit.value(BRANCH) + "|" + unit.value(CURRENCY);
  }

  @Override
  public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
    MigTrueup trueup =
        trueups
            .findByBatchId(ctx.batch().getId())
            .filter(t -> t.getStatus() == MigTrueup.Status.APPROVED)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "MIG_TRUEUP_NOT_APPROVED",
                        "The opening-balance adjustment of batch "
                            + ctx.batchNo()
                            + " is not approved by the Head of Comptrollership"));
    String branch = Values.code(unit.value(BRANCH));
    String currency = Values.code(unit.value(CURRENCY));
    Long branchId =
        branches
            .findByCompanyIdAndCode(ctx.companyId(), branch)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "MIG_BRANCH_UNKNOWN", "Branch " + branch + " not found"))
            .getId();
    List<OpeningJournals.Amounts> amounts = new ArrayList<>();
    for (StageRow row : JournalGroups.lines(unit)) {
      Map<String, String> r = row.getMappedPayload();
      amounts.add(
          OpeningJournals.Amounts.of(
              Values.text(r.get("legacy_account_code")),
              Values.text(r.get("cost_center")),
              Values.amount(r.get("debit_fc")),
              Values.amount(r.get("credit_fc")),
              Values.amount(r.get("debit_php")),
              Values.amount(r.get("credit_php"))));
    }
    LocalDate date = parameters.openingValueDate();
    JournalBatch batch =
        journals.post(
            new OpeningJournals.Header(
                ctx.companyId(),
                branchId,
                currency,
                date,
                trueup.getReference(),
                trueup.getReference() + "-" + branch + "-" + currency,
                "Opening-balance adjustment " + trueup.getReference() + " (FY2027 true-up)"),
            amounts,
            true);
    for (StageRow detail : unit.rows(DETAIL)) {
      adjust(trueup, detail.getMappedPayload(), date);
    }
    return LoadOutcome.of(
        GlOpeningLoader.JOURNAL, batch.getId(), batch.getBatchNo(), batch.getVersion());
  }

  /** One open-item detail line: the legacy invoice component and its event. */
  private void adjust(MigTrueup trueup, Map<String, String> d, LocalDate date) {
    String ref = Values.code(d.get("item_ref"));
    OpsInvoice invoice = invoiceOf(Values.code(d.get("item_kind")), ref);
    String component = Values.code(d.get("component")).toUpperCase(Locale.ROOT);
    boolean writeOff = "WRITTEN_OFF".equals(Values.code(d.get("movement")));
    BigDecimal amount = Values.amount(d.get("amount"));
    BigDecimal change = writeOff ? amount.negate() : amount;
    String sourceRef = "MIG:TU:" + trueup.getTrueupNo() + ":" + ref + ":" + component;
    Map<String, BigDecimal> event = new LinkedHashMap<>();
    if (UNREALIZED.equals(component) || DEFERRED_VAT.equals(component)) {
      event.put(component, change);
    } else {
      LedgerComponent c = LedgerComponent.valueOf(component);
      requireInRange(invoice, c, change, ref);
      Map<LedgerComponent, BigDecimal> amounts = new EnumMap<>(LedgerComponent.class);
      amounts.put(c, writeOff ? amount : change);
      intake.adjustOpening(
          invoice.getInvoiceNo(),
          writeOff ? MovementType.LEGACY_WRITTEN_OFF : MovementType.LEGACY_ADJUSTED,
          amounts,
          sourceRef,
          date);
      event.put(
          LegacyInvoicePosting.eventComponent(c),
          c == LedgerComponent.WTAX ? change.negate() : change);
    }
    posting.trueUp(invoice, event, sourceRef, date);
  }

  private OpsInvoice invoiceOf(String kind, String ref) {
    if (!"INVOICE".equals(kind)) {
      throw new BusinessRuleException(
          "MIG_TRUEUP_ITEM_KIND",
          "Unapplied payment " + ref + " is adjusted with the unapplied payments object");
    }
    return invoices.findByLegacyLegacyInvoiceNoOrderByIdAsc(ref).stream()
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "MIG_TRUEUP_ITEM_MISSING", "Legacy invoice " + ref + " is not loaded"));
  }

  /** An adjustment never takes an open item below zero (C5; DQ-056). */
  private static void requireInRange(
      OpsInvoice invoice, LedgerComponent c, BigDecimal change, String ref) {
    BigDecimal after = invoice.balances().getOrDefault(c, BigDecimal.ZERO).add(change);
    if (after.signum() < 0) {
      throw new BusinessRuleException(
          "MIG_TRUEUP_OUT_OF_RANGE",
          "The adjustment would take "
              + c
              + " of invoice "
              + ref
              + " below zero; it moved in BIBS since go-live - settle it with Comptrollership");
    }
  }
}
