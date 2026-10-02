package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied.UnappliedSpec;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedLegacy;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import com.iortatechnxt.brokerverse.cashiering.service.UnappliedService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.service.LoadContext;
import com.iortatechnxt.brokerverse.migration.load.service.LoadOutcome;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import com.iortatechnxt.brokerverse.migration.load.service.XrefService;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerContext;
import com.iortatechnxt.brokerverse.opsledger.service.BookRates;
import com.iortatechnxt.brokerverse.opsledger.service.LegacyInvoiceIntake;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Loader of the legacy unapplied premium payments (object F02; DATA_MIGRATION_DESIGN 14.3). Each
 * item is created in Cashiering through {@link UnappliedService#createMigrated} (origin MIGRATED,
 * ledger context LEGACY, the legacy acknowledgement receipt kept and no BIBS receipt issued), with
 * its open balance at cut-over, the legacy disposition as a hint and the invoice, cover, PN and
 * bank references for the automatch; its opening entry {@code MIG_UPP_OPENING} (migration clearing
 * against legacy unapplied collections) is posted on the opening value date.
 */
@Component
public class UppLoader implements MigrationLoader {

  /** Entity of an unapplied item in the cross-reference. */
  public static final String ENTITY = "Unapplied";

  /** Opening event of a legacy unapplied payment. */
  public static final String EVENT = "MIG_UPP_OPENING";

  private static final String AMOUNT = "amount";
  private static final String BALANCE = "balance";
  private static final int MAX_REMARKS = 250;

  private final UnappliedService unapplied;
  private final UnappliedRepository items;
  private final AccountingEventPublisher publisher;
  private final BookRates rates;
  private final BranchRepository branches;
  private final XrefService xrefs;
  private final MigrationParameters parameters;

  /**
   * Creates the loader.
   *
   * @param unapplied unapplied workbench
   * @param items unapplied items (read)
   * @param publisher accounting engine
   * @param rates book rates
   * @param branches branches
   * @param xrefs cross-references (migrated clients)
   * @param parameters migration parameters
   */
  @SuppressWarnings("java:S107") // collaborators
  public UppLoader(
      UnappliedService unapplied,
      UnappliedRepository items,
      AccountingEventPublisher publisher,
      BookRates rates,
      BranchRepository branches,
      XrefService xrefs,
      MigrationParameters parameters) {
    this.unapplied = unapplied;
    this.items = items;
    this.publisher = publisher;
    this.rates = rates;
    this.branches = branches;
    this.xrefs = xrefs;
    this.parameters = parameters;
  }

  @Override
  public String objectCode() {
    return "F02";
  }

  @Override
  public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
    Map<String, String> v = unit.values();
    String branch = Values.code(v.get("branch_code"));
    Long branchId =
        branches
            .findByCompanyIdAndCode(ctx.companyId(), branch)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "MIG_BRANCH_UNKNOWN", "Branch " + branch + " not found"))
            .getId();
    String legacyClient = Values.code(v.get("legacy_client_no"));
    String client =
        legacyClient == null
            ? null
            : xrefs
                .live(ctx.companyId(), unit.sourceSystem(), "C01", legacyClient)
                .map(KeyXref::getTargetCode)
                .orElseThrow(
                    () ->
                        new BusinessRuleException(
                            "MIG_C01_NOT_LOADED", "Client " + legacyClient + " is not migrated"));
    BigDecimal balance = Values.amount(v.get(BALANCE));
    Unapplied item =
        unapplied.createMigrated(
            ctx.companyId(),
            branchId,
            new UnappliedSpec(
                null,
                null,
                null,
                firstInvoice(v),
                client,
                Values.text(v.get("payor_name")),
                Values.text(v.get("sales_unit_code")),
                Values.code(v.get("currency")),
                Values.amount(v.get(AMOUNT)),
                Values.text(v.get("disposition_type")),
                null,
                null,
                remarks(v)),
            new UnappliedLegacy(
                unit.sourceSystem(),
                unit.legacyKey(),
                ctx.batchNo(),
                LedgerContext.LEGACY,
                Values.text(v.get("legacy_ar_no")),
                Values.date(v.get("legacy_ar_date")).orElse(null),
                matchRefs(v)));
    unapplied.openAt(item, balance);
    publish(item, balance, "", parameters.openingValueDate());
    return LoadOutcome.of(ENTITY, item.getId(), item.getReference(), item.getVersion());
  }

  private static String firstInvoice(Map<String, String> v) {
    List<String> invoices = Values.items(v.get("ref_invoice_nos"));
    return invoices.isEmpty() ? null : invoices.get(0);
  }

  private static String matchRefs(Map<String, String> v) {
    List<String> refs = new ArrayList<>();
    for (String column :
        List.of("ref_invoice_nos", "ref_cover_nos", "ref_pn_nos", "ref_bank_reference")) {
      refs.addAll(Values.items(v.get(column)));
    }
    String joined = String.join(",", refs);
    return joined.isEmpty() ? null : joined;
  }

  private static String remarks(Map<String, String> v) {
    List<String> parts = new ArrayList<>();
    Optional.ofNullable(Values.text(v.get("legacy_status")))
        .ifPresent(s -> parts.add("Legacy status " + s));
    Optional.ofNullable(Values.text(v.get("disposition_status")))
        .ifPresent(s -> parts.add("disposition " + s));
    Optional.ofNullable(Values.text(v.get("disposition_details"))).ifPresent(parts::add);
    Optional.ofNullable(Values.text(v.get("remarks"))).ifPresent(parts::add);
    String text = String.join("; ", parts);
    return text.length() > MAX_REMARKS ? text.substring(0, MAX_REMARKS) : text;
  }

  private void publish(Unapplied item, BigDecimal balance, String suffix, LocalDate date) {
    if (balance.signum() == 0) {
      return;
    }
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    amounts.put(LedgerContext.LEGACY.component("AMOUNT"), balance);
    amounts.put("CLEARING", balance.negate());
    publisher.publish(
        rates.price(
            new BusinessEvent(
                EVENT,
                item.getCompanyId(),
                item.getBranchId(),
                date,
                item.getCurrency(),
                LegacyInvoiceIntake.MODULE,
                "MIG:UPP:" + item.getReference() + suffix,
                item.getLegacy().legacyArNo(),
                item.getClientCode(),
                null,
                null,
                "Legacy unapplied payment " + item.getLegacy().legacyRef(),
                amounts,
                Map.of(),
                Map.of())));
  }

  @Override
  public List<String> reconciledColumns() {
    return List.of(AMOUNT, BALANCE);
  }

  @Override
  public Map<String, String> readBack(KeyXref entry) {
    return items
        .findById(entry.getTargetId())
        .map(
            u -> {
              Map<String, String> m = new HashMap<>();
              m.put(AMOUNT, u.getAmount().toPlainString());
              m.put(BALANCE, u.getBalance().toPlainString());
              return m;
            })
        .orElse(Map.of());
  }

  @Override
  public boolean reversible() {
    return true;
  }

  @Override
  public boolean changedSinceLoad(KeyXref entry) {
    return items
        .findById(entry.getTargetId())
        .map(u -> !Unapplied.STAGE_INITIAL.equals(u.getStage()))
        .orElse(false);
  }

  @Override
  public boolean compensate(KeyXref entry, LoadContext ctx) {
    Unapplied item =
        items
            .findById(entry.getTargetId())
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "MIG_UPP_MISSING",
                        "Unapplied item " + entry.getTargetCode() + " not found"));
    publish(item, item.getBalance().negate(), ":RB", ctx.businessDate());
    unapplied.rollbackMigrated(item, ctx.batchNo());
    return true;
  }

  @Override
  public Optional<BigDecimal> targetTotal(AmountMeasure measure, List<KeyXref> loaded) {
    if (!"F02".equals(measure.layoutCode()) || !reconciledColumns().contains(measure.column())) {
      return Optional.empty();
    }
    BigDecimal total = BigDecimal.ZERO;
    for (Unapplied u : items.findAllById(loaded.stream().map(KeyXref::getTargetId).toList())) {
      if (measure.currency() == null || measure.currency().equals(u.getCurrency())) {
        total = total.add(AMOUNT.equals(measure.column()) ? u.getAmount() : u.getBalance());
      }
    }
    return Optional.of(total);
  }
}
