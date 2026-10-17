package com.iortatechnxt.brokerverse.migration.recon.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.recon.domain.ReconLine;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Level L5 of the GL objects (trial balance G01 and true-ups G03; DATA_MIGRATION_DESIGN 12 and
 * 14.2): after the open items and the trial balance are loaded, and again after each true-up, the
 * Migration Clearing account is 0.00 per branch and currency. A non-zero balance means the detail
 * and the trial balance disagree; it raises the alert {@code MIG_CLEARING_NOT_ZERO} and is a go /
 * no-go criterion.
 */
@Component
public class ClearingCheck implements ReconCheck {

  /** The Migration Clearing account of the seed chart (code assigned by Comptrollership). */
  public static final String CLEARING_ACCOUNT = "LGC-CLR";

  private static final Set<String> OBJECTS = Set.of("G01", "G03");

  private static final String SQL =
      "select b.code as branch, e.currency, coalesce(sum(e.debit_fc - e.credit_fc), 0) as amount"
          + " from gl_ledger_entry e join coa_account a on a.id = e.account_id"
          + " join org_branch b on b.id = e.branch_id"
          + " where e.company_id = ? and a.code = ? group by b.code, e.currency"
          + " order by b.code, e.currency";

  private final JdbcTemplate jdbc;
  private final AlertService alerts;

  /**
   * Creates the check.
   *
   * @param jdbc JDBC
   * @param alerts alerts
   */
  public ClearingCheck(JdbcTemplate jdbc, AlertService alerts) {
    this.jdbc = jdbc;
    this.alerts = alerts;
  }

  @Override
  public boolean appliesTo(String objectCode) {
    return OBJECTS.contains(objectCode);
  }

  @Override
  public List<ReconLineSpec> lines(MigBatch batch) {
    List<ReconLineSpec> out = new ArrayList<>();
    jdbc.query(
        SQL,
        rs -> {
          BigDecimal amount = rs.getBigDecimal("amount");
          String where = rs.getString("branch") + " " + rs.getString("currency");
          out.add(
              new ReconLineSpec(
                  "L5",
                  "Migration Clearing is zero (" + where + ")",
                  rs.getString("currency"),
                  ReconLine.Values.of(BigDecimal.ZERO, BigDecimal.ZERO, amount),
                  "Balance of the Migration Clearing account for branch " + where));
          if (amount.signum() != 0) {
            alerts.raise(
                MigrationCodes.ALERT_CLEARING,
                new AlertFacts(
                    batch.getCompanyId(),
                    null,
                    MigrationCodes.ENTITY_BATCH,
                    batch.getBatchNo(),
                    "Migration Clearing of " + where + " is " + amount.toPlainString(),
                    amount,
                    MigrationCodes.ALERT_CLEARING + ":" + where));
          }
        },
        batch.getCompanyId(),
        CLEARING_ACCOUNT);
    return out;
  }
}
