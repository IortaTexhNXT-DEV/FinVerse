package com.iortatechnxt.brokerverse.migration.recon.service;

import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import com.iortatechnxt.brokerverse.migration.recon.domain.ReconLine;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

/**
 * Level L5 of the legacy unapplied payments (object F02; DATA_MIGRATION_DESIGN 12 and 14.3): per
 * currency, the GL balance of the legacy unapplied collections account (2206 in the seed chart)
 * equals the balances of the unapplied items of ledger context LEGACY in Cashiering.
 */
@Component
public class UppControlCheck implements ReconCheck {

  /** Legacy unapplied collections of the seed chart (code assigned by Comptrollership). */
  public static final String ACCOUNT = "2206";

  private static final String DETAIL =
      "select currency, coalesce(sum(balance), 0) as amount from csh_unapplied"
          + " where company_id = ? and ledger_context = 'LEGACY' group by currency";

  private static final String GL =
      "select e.currency, coalesce(sum(e.credit_fc - e.debit_fc), 0) as amount"
          + " from gl_ledger_entry e join coa_account a on a.id = e.account_id"
          + " where e.company_id = ? and a.code = ? group by e.currency";

  private final JdbcTemplate jdbc;

  /**
   * Creates the check.
   *
   * @param jdbc JDBC
   */
  public UppControlCheck(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean appliesTo(String objectCode) {
    return "F02".equals(objectCode);
  }

  @Override
  public List<ReconLineSpec> lines(MigBatch batch) {
    Map<String, BigDecimal> detail = new HashMap<>();
    jdbc.query(DETAIL, into(detail), batch.getCompanyId());
    Map<String, BigDecimal> gl = new HashMap<>();
    jdbc.query(GL, into(gl), batch.getCompanyId(), ACCOUNT);
    Set<String> currencies = new TreeSet<>(detail.keySet());
    currencies.addAll(gl.keySet());
    List<ReconLineSpec> out = new ArrayList<>();
    for (String currency : currencies) {
      BigDecimal expected = detail.getOrDefault(currency, BigDecimal.ZERO);
      out.add(
          new ReconLineSpec(
              "L5",
              "Legacy unapplied collections " + ACCOUNT + " = legacy unapplied payments",
              currency,
              ReconLine.Values.of(expected, expected, gl.getOrDefault(currency, BigDecimal.ZERO)),
              "GL balance of " + ACCOUNT + " against the balances of the legacy unapplied items"));
    }
    return out;
  }

  private static RowCallbackHandler into(Map<String, BigDecimal> out) {
    return rs -> out.put(rs.getString("currency"), rs.getBigDecimal("amount"));
  }
}
