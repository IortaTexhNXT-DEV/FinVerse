package com.iortatechnxt.brokerverse.fixedasset.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.fixedasset.domain.AssetCategory;
import com.iortatechnxt.brokerverse.fixedasset.domain.AssetStatus;
import com.iortatechnxt.brokerverse.fixedasset.domain.DepreciationLine;
import com.iortatechnxt.brokerverse.fixedasset.domain.DepreciationLineRepository;
import com.iortatechnxt.brokerverse.fixedasset.domain.DepreciationRun;
import com.iortatechnxt.brokerverse.fixedasset.domain.DepreciationRunRepository;
import com.iortatechnxt.brokerverse.fixedasset.domain.FixedAsset;
import com.iortatechnxt.brokerverse.fixedasset.domain.FixedAssetRepository;
import com.iortatechnxt.brokerverse.fixedasset.service.AssetAccounting.Posting;
import com.iortatechnxt.brokerverse.fixedasset.service.DepreciationCalculator.Basis;
import com.iortatechnxt.brokerverse.fixedasset.service.DepreciationCalculator.Charge;
import com.iortatechnxt.brokerverse.fixedasset.service.DepreciationCalculator.Position;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monthly depreciation run: preview, then post once per period.
 *
 * <p>Every capitalized, depreciable asset (capitalized on or before the period end) is charged for
 * each month from the month after its last depreciated month up to the period (catching up months
 * missed, e.g. an asset capitalized late). Charges are posted as one ASSET_DEPRECIATION journal per
 * branch, category and cost centre, valued at the period end. A period can be posted only once:
 * posting it again returns the existing run.
 */
@Service
@Transactional
public class DepreciationService {

  private static final Set<AssetStatus> DEPRECIABLE =
      EnumSet.of(AssetStatus.ACTIVE, AssetStatus.TRANSFERRED);

  private final FixedAssetRepository assets;
  private final DepreciationRunRepository runs;
  private final DepreciationLineRepository lines;
  private final AssetAccounting accounting;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param assets asset repository
   * @param runs run repository
   * @param lines run line repository
   * @param accounting accounting helper
   * @param audit audit trail
   */
  public DepreciationService(
      FixedAssetRepository assets,
      DepreciationRunRepository runs,
      DepreciationLineRepository lines,
      AssetAccounting accounting,
      AuditTrailService audit) {
    this.assets = assets;
    this.runs = runs;
    this.lines = lines;
    this.accounting = accounting;
    this.audit = audit;
  }

  /**
   * Computes the depreciation due for a period without posting.
   *
   * @param companyId company
   * @param period month
   * @return proposed charges
   */
  @Transactional(readOnly = true)
  public List<Proposal> preview(Long companyId, YearMonth period) {
    return assets.findByCompanyIdAndStatusInOrderByTagNo(companyId, DEPRECIABLE).stream()
        .filter(a -> !a.getCapitalizationDate().isAfter(period.atEndOfMonth()))
        .map(a -> proposal(a, period))
        .filter(Objects::nonNull)
        .toList();
  }

  /**
   * Posts the depreciation of a period (idempotent: returns the existing run when already posted).
   *
   * @param companyId company
   * @param period month
   * @return run
   */
  public DepreciationRun post(Long companyId, YearMonth period) {
    Optional<DepreciationRun> existing =
        runs.findByCompanyIdAndPeriod(companyId, period.toString());
    if (existing.isPresent()) {
      return existing.get();
    }
    DepreciationRun run = new DepreciationRun(companyId, period);
    Map<String, List<Proposal>> groups = new LinkedHashMap<>();
    for (Proposal p : preview(companyId, period)) {
      FixedAsset a = p.asset();
      String key = a.getBranchId() + ":" + a.getCategory().getId() + ":" + a.getCostCenter();
      groups.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
    }
    groups.forEach((key, group) -> postGroup(run, period, key, group));
    DepreciationRun saved = runs.save(run);
    audit.record(
        "DepreciationRun",
        period.toString(),
        AuditAction.CREATE,
        "Posted depreciation "
            + period
            + ": "
            + saved.getAssetCount()
            + " assets, "
            + saved.getTotalDepreciation().toPlainString());
    return saved;
  }

  /**
   * Finds the run of a period.
   *
   * @param companyId company
   * @param period month
   * @return run if posted
   */
  @Transactional(readOnly = true)
  public Optional<DepreciationRun> findRun(Long companyId, YearMonth period) {
    return runs.findByCompanyIdAndPeriod(companyId, period.toString());
  }

  /**
   * Lists posted runs.
   *
   * @param companyId company
   * @return runs, newest first
   */
  @Transactional(readOnly = true)
  public List<DepreciationRun> runs(Long companyId) {
    return runs.findByCompanyIdOrderByPeriodDesc(companyId);
  }

  /**
   * Lines of a run.
   *
   * @param runId run
   * @return lines
   */
  @Transactional(readOnly = true)
  public List<DepreciationLine> lines(Long runId) {
    if (!runs.existsById(runId)) {
      throw new ResourceNotFoundException("DepreciationRun", runId);
    }
    return lines.findByRunIdOrderById(runId);
  }

  private void postGroup(DepreciationRun run, YearMonth period, String key, List<Proposal> group) {
    FixedAsset first = group.get(0).asset();
    AssetCategory category = first.getCategory();
    BigDecimal total =
        group.stream().map(Proposal::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    JournalBatch batch =
        accounting.publish(
            run.getCompanyId(),
            category,
            first.getCostCenter(),
            null,
            new Posting(
                "ASSET_DEPRECIATION",
                first.getBranchId(),
                run.getPeriodEnd(),
                "FA-DEPR:" + period + ":" + key,
                "DEPR-" + period,
                "Depreciation " + period + " - " + category.getName(),
                Map.of("DEPRECIATION", total),
                Map.of()));
    for (Proposal p : group) {
      p.asset().applyDepreciation(period, p.months(), p.amount());
      run.addLine(new DepreciationLine(run, p.asset(), p.months(), p.amount(), batch.getBatchNo()));
    }
  }

  private static Proposal proposal(FixedAsset a, YearMonth period) {
    int due = (int) ChronoUnit.MONTHS.between(a.lastDepreciatedMonth(), period);
    if (due <= 0) {
      return null;
    }
    Charge charge = DepreciationCalculator.charge(Basis.of(a), Position.of(a), due);
    if (charge.amount().signum() == 0) {
      return null;
    }
    return new Proposal(
        a,
        charge.months(),
        charge.amount(),
        a.getAccumulatedDepreciation().add(charge.amount()),
        a.netBookValue().subtract(charge.amount()));
  }

  /**
   * A proposed depreciation charge.
   *
   * @param asset asset
   * @param months months charged
   * @param amount charge
   * @param accumulatedAfter accumulated depreciation after the charge
   * @param netBookValueAfter net book value after the charge
   */
  public record Proposal(
      FixedAsset asset,
      int months,
      BigDecimal amount,
      BigDecimal accumulatedAfter,
      BigDecimal netBookValueAfter) {}
}
