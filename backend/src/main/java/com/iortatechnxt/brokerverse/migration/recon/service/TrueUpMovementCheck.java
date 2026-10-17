package com.iortatechnxt.brokerverse.migration.recon.service;

import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.intake.domain.RowStatus;
import com.iortatechnxt.brokerverse.migration.intake.domain.StageRow;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.load.service.BatchPlanService;
import com.iortatechnxt.brokerverse.migration.recon.domain.ReconLine;
import com.iortatechnxt.brokerverse.migration.trueup.domain.MigTrueup;
import com.iortatechnxt.brokerverse.migration.trueup.domain.MigTrueupRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

/**
 * The movement check of a FY2027 true-up (object G03; DATA_MIGRATION_DESIGN 17.7): per BIBS
 * account, branch and currency, the adjustment lines of true-up n equal the legacy trial balance of
 * true-up n less the previous legacy trial balance (the provisional opening or the previous
 * true-up), both mapped through code map GL_ACCOUNT, in pesos.
 */
@Component
public class TrueUpMovementCheck implements ReconCheck {

  private static final List<String> VERSIONS = List.of("PROVISIONAL", "TU1", "TU2", "TU3", "FINAL");

  private static final String TB =
      "select l.account_code, b.code as branch, l.currency,"
          + " coalesce(sum(l.debit_php - l.credit_php), 0) as amount"
          + " from mig_tb_line l join org_branch b on b.id = l.branch_id"
          + " where l.company_id = ? and l.tb_version = ? and l.rolled_back_at is null"
          + " group by l.account_code, b.code, l.currency";

  private final MigTrueupRepository trueups;
  private final BatchPlanService plans;
  private final JdbcTemplate jdbc;

  /**
   * Creates the check.
   *
   * @param trueups true-ups
   * @param plans batch rows
   * @param jdbc JDBC (legacy trial balances)
   */
  public TrueUpMovementCheck(
      MigTrueupRepository trueups, BatchPlanService plans, JdbcTemplate jdbc) {
    this.trueups = trueups;
    this.plans = plans;
    this.jdbc = jdbc;
  }

  @Override
  public boolean appliesTo(String objectCode) {
    return "G03".equals(objectCode);
  }

  @Override
  public List<ReconLineSpec> lines(MigBatch batch) {
    Optional<MigTrueup> trueup = trueups.findByBatchId(batch.getId());
    if (trueup.isEmpty()) {
      return List.of();
    }
    String version =
        "F".equals(trueup.get().getTrueupNo()) ? "FINAL" : "TU" + trueup.get().getTrueupNo();
    Map<String, BigDecimal> current = tb(batch.getCompanyId(), version);
    if (current.isEmpty()) {
      return List.of(
          new ReconLineSpec(
              "L5",
              "Legacy trial balance " + version + " loaded",
              null,
              ReconLine.Values.check(false),
              "Load the legacy trial balance "
                  + version
                  + " (object G01) before the reconciliation"));
    }
    Map<String, BigDecimal> previous = previous(batch.getCompanyId(), version);
    Map<String, BigDecimal> adjustments = adjustments(batch);
    Set<String> keys = new TreeSet<>(current.keySet());
    keys.addAll(previous.keySet());
    keys.addAll(adjustments.keySet());
    List<ReconLineSpec> out = new ArrayList<>();
    for (String key : keys) {
      BigDecimal movement =
          current
              .getOrDefault(key, BigDecimal.ZERO)
              .subtract(previous.getOrDefault(key, BigDecimal.ZERO));
      BigDecimal posted = adjustments.getOrDefault(key, BigDecimal.ZERO);
      if (movement.signum() != 0 || posted.signum() != 0) {
        String[] k = key.split("\\|");
        out.add(
            new ReconLineSpec(
                "L5",
                "Movement of " + k[0] + " (" + k[1] + ")",
                k[2],
                ReconLine.Values.of(movement, movement, posted),
                "Legacy trial balance "
                    + version
                    + " less the previous version against the adjustment lines"));
      }
    }
    return out;
  }

  private Map<String, BigDecimal> previous(Long companyId, String version) {
    for (int i = VERSIONS.indexOf(version) - 1; i >= 0; i--) {
      Map<String, BigDecimal> tb = tb(companyId, VERSIONS.get(i));
      if (!tb.isEmpty()) {
        return tb;
      }
    }
    return Map.of();
  }

  private Map<String, BigDecimal> tb(Long companyId, String version) {
    Map<String, BigDecimal> out = new HashMap<>();
    RowCallbackHandler row =
        rs ->
            out.merge(
                rs.getString("account_code")
                    + "|"
                    + rs.getString("branch")
                    + "|"
                    + rs.getString("currency"),
                rs.getBigDecimal("amount"),
                BigDecimal::add);
    jdbc.query(TB, row, companyId, version);
    return out;
  }

  private Map<String, BigDecimal> adjustments(MigBatch batch) {
    Map<String, BigDecimal> out = new HashMap<>();
    for (StageRow r :
        plans.rowsByLayout(batch, EnumSet.of(RowStatus.LOADED)).getOrDefault("G03", List.of())) {
      Map<String, String> v = r.getMappedPayload();
      out.merge(
          v.get("legacy_account_code") + "|" + v.get("branch_code") + "|" + v.get("currency"),
          Values.amount(v.get("debit_php")).subtract(Values.amount(v.get("credit_php"))),
          BigDecimal::add);
    }
    return out;
  }
}
