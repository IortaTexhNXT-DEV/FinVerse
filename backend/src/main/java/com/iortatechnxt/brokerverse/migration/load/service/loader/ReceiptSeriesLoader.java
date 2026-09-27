package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptSeries;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSeriesService;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSeriesService.SeriesRequest;
import com.iortatechnxt.brokerverse.common.domain.RecordOrigin;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.service.LoadContext;
import com.iortatechnxt.brokerverse.migration.load.service.LoadOutcome;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Loader of the receipt series in use at the freeze (object R11; DATA_MIGRATION_DESIGN section 2):
 * each AR or OR series is created through {@link ReceiptSeriesService} pending authorisation, with
 * its BIR authority to print and range, continues from the next number the legacy system would have
 * issued, and carries origin MIGRATED. A rolled-back batch deactivates the series.
 */
@Component
public class ReceiptSeriesLoader implements MigrationLoader {

  private static final String NEXT = "next_no";

  private final ReceiptSeriesService series;
  private final BranchRepository branches;

  /**
   * Creates the loader.
   *
   * @param series receipt series
   * @param branches branches
   */
  public ReceiptSeriesLoader(ReceiptSeriesService series, BranchRepository branches) {
    this.series = series;
    this.branches = branches;
  }

  @Override
  public String objectCode() {
    return "R11";
  }

  @Override
  public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
    Map<String, String> v = unit.values();
    String branchCode = Values.code(v.get("branch_code"));
    Long branchId =
        branches
            .findByCompanyIdAndCode(ctx.companyId(), branchCode)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "MIG_BRANCH_UNKNOWN", "Branch " + branchCode + " not found"))
            .getId();
    ReceiptKind kind = ReceiptKind.valueOf(Values.code(v.get("kind")).toUpperCase(Locale.ROOT));
    String prefix = Values.text(v.get("prefix"));
    ReceiptSeries created =
        series.create(
            new SeriesRequest(
                ctx.companyId(),
                branchId,
                kind,
                prefix == null ? kind + "-" + branchCode + "-" : prefix,
                number(v, "from_no"),
                number(v, "to_no"),
                Values.text(v.get("atp_no")),
                0));
    created.continueFrom(number(v, NEXT));
    created.markMigrated(
        RecordOrigin.migrated(unit.sourceSystem(), unit.legacyKey(), ctx.batchNo()));
    return LoadOutcome.of("ReceiptSeries", created.getId(), created.getPrefix(), null);
  }

  private static long number(Map<String, String> v, String column) {
    return Values.decimal(v.get(column))
        .orElseThrow(
            () -> new BusinessRuleException("MIG_NUMBER_REQUIRED", "The " + column + " is missing"))
        .longValueExact();
  }

  @Override
  public List<String> reconciledColumns() {
    return List.of(NEXT);
  }

  @Override
  public Map<String, String> readBack(KeyXref entry) {
    return Map.of(NEXT, Long.toString(series.get(entry.getTargetId()).getNextNo()));
  }

  @Override
  public boolean reversible() {
    return true;
  }

  @Override
  public boolean compensate(KeyXref entry, LoadContext ctx) {
    ReceiptSeries s = series.get(entry.getTargetId());
    if (ctx.batchNo().equals(s.getRecordOrigin().migrationBatch())) {
      s.deactivate();
    }
    return true;
  }
}
