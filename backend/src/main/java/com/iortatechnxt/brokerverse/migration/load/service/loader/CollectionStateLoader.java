package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.collections.legacy.domain.LegacyState;
import com.iortatechnxt.brokerverse.collections.legacy.service.LegacyItemStateService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.service.LoadContext;
import com.iortatechnxt.brokerverse.migration.load.service.LoadOutcome;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Loader of the open collection follow-up of the legacy invoices (object F03; DATA_MIGRATION_DESIGN
 * 14.4 J): the rows of a legacy invoice - latest disposition, promises, installments and collector
 * - are handed to the Collections service {@link LegacyItemStateService}, which keeps them on the
 * account and assigns the item to its collector. Only open state is carried; the history stays in
 * the archive.
 */
@Component
public class CollectionStateLoader implements MigrationLoader {

  /** Entity of the carried follow-up in the cross-reference. */
  public static final String ENTITY = "LegacyFollowUp";

  private static final String INVOICE = "legacy_invoice_no";

  private final LegacyItemStateService states;
  private final OpsInvoiceRepository invoices;

  /**
   * Creates the loader.
   *
   * @param states Collections legacy follow-up
   * @param invoices ledger invoices
   */
  public CollectionStateLoader(LegacyItemStateService states, OpsInvoiceRepository invoices) {
    this.states = states;
    this.invoices = invoices;
  }

  @Override
  public String objectCode() {
    return "F03";
  }

  @Override
  public List<LoadUnit> group(List<LoadUnit> units) {
    return JournalGroups.group(units, u -> u.sourceSystem() + "|" + u.value(INVOICE));
  }

  @Override
  public String partitionKey(LoadUnit unit) {
    return unit.value(INVOICE);
  }

  @Override
  public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
    String legacyNo = Values.code(unit.value(INVOICE));
    OpsInvoice invoice =
        invoices.findByLegacyLegacyInvoiceNoOrderByIdAsc(legacyNo).stream()
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "MIG_F01_NOT_LOADED", "Legacy invoice " + legacyNo + " is not loaded"));
    List<LegacyState.Row> rows = new ArrayList<>();
    for (StageRow r : JournalGroups.lines(unit)) {
      rows.add(row(r.getMappedPayload()));
    }
    List<LegacyState> saved =
        states.record(
            new LegacyState.Key(
                ctx.companyId(),
                invoice.getInvoiceNo(),
                legacyNo,
                invoice.getRecordOrigin().sourceSystem(),
                ctx.batchNo()),
            rows);
    return LoadOutcome.of(ENTITY, saved.get(0).getId(), invoice.getInvoiceNo(), 0L);
  }

  private static LegacyState.Row row(Map<String, String> v) {
    return new LegacyState.Row(
        Values.code(v.get("record_type")).toUpperCase(Locale.ROOT),
        Values.decimal(v.get("seq_no")).map(BigDecimal::intValue).orElse(1),
        Values.text(v.get("disposition_code")),
        Values.date(v.get("disposition_date")).orElse(null),
        Values.date(v.get("promise_date")).orElse(null),
        Values.decimal(v.get("promise_amount")).orElse(null),
        Values.decimal(v.get("installment_no")).map(BigDecimal::intValue).orElse(null),
        Values.date(v.get("installment_due")).orElse(null),
        Values.decimal(v.get("installment_amount")).orElse(null),
        Values.text(v.get("collector_user_id")),
        Values.text(v.get("remarks")));
  }

  @Override
  public boolean reversible() {
    return true;
  }

  @Override
  public boolean compensate(KeyXref entry, LoadContext ctx) {
    states.rollback(ctx.batchNo());
    return true;
  }
}
