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
 * Level L5 of the open legacy invoices (object F01; DATA_MIGRATION_DESIGN 12 and 14.2): per legacy
 * control account and currency, the GL balance equals the open detail of the legacy invoices in the
 * Operations ledger - premium receivable by component (1215.01-.06), PR 2307 (1216), due to
 * insurers (LGC-DTIP) and commission receivable net of withholding tax (LGC-COMM). The account
 * codes are those of the seed chart; production uses the codes assigned by Comptrollership through
 * the same rules.
 */
@Component
public class LegacyControlCheck implements ReconCheck {

  private static final String LEVEL = "L5";

  /** Control account, sign of its natural balance (1 debit, -1 credit) and ledger expression. */
  private static final List<Control> CONTROLS =
      List.of(
          new Control("1215.01", 1, List.of("BASIC")),
          new Control("1215.02", 1, List.of("DST")),
          new Control("1215.03", 1, List.of("PREMIUM_TAX_VAT")),
          new Control("1215.04", 1, List.of("LGT")),
          new Control("1215.05", 1, List.of("FST")),
          new Control("1215.06", 1, List.of("OTHER")),
          new Control("1216", 1, List.of("PR2307")),
          new Control("LGC-DTIP", -1, List.of("DTIP")),
          new Control("LGC-COMM", 1, List.of("COMMISSION", "COMMISSION_VAT", "WTAX")));

  private static final String DETAIL =
      "select i.currency, coalesce(sum(case when c.component = 'WTAX' then -c.balance"
          + " else c.balance end), 0) as amount"
          + " from ops_invoice i join ops_invoice_component c on c.invoice_id = i.id"
          + " where i.company_id = ? and i.ledger_context = 'LEGACY' and c.component in (?, ?, ?)"
          + " group by i.currency";

  private static final String GL =
      "select e.currency, coalesce(sum(e.debit_fc - e.credit_fc), 0) as amount"
          + " from gl_ledger_entry e join coa_account a on a.id = e.account_id"
          + " where e.company_id = ? and a.code = ? group by e.currency";

  private final JdbcTemplate jdbc;

  /**
   * Creates the check.
   *
   * @param jdbc JDBC
   */
  public LegacyControlCheck(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean appliesTo(String objectCode) {
    return "F01".equals(objectCode);
  }

  @Override
  public List<ReconLineSpec> lines(MigBatch batch) {
    List<ReconLineSpec> out = new ArrayList<>();
    for (Control control : CONTROLS) {
      List<String> in = control.components();
      Map<String, BigDecimal> detail = new HashMap<>();
      jdbc.query(
          DETAIL,
          into(detail),
          batch.getCompanyId(),
          in.get(0),
          in.get(Math.min(1, in.size() - 1)),
          in.get(in.size() - 1));
      Map<String, BigDecimal> gl = new HashMap<>();
      jdbc.query(GL, into(gl), batch.getCompanyId(), control.account());
      Set<String> currencies = new TreeSet<>(detail.keySet());
      currencies.addAll(gl.keySet());
      for (String currency : currencies) {
        BigDecimal expected = detail.getOrDefault(currency, BigDecimal.ZERO);
        BigDecimal posted =
            gl.getOrDefault(currency, BigDecimal.ZERO).multiply(BigDecimal.valueOf(control.sign()));
        out.add(
            new ReconLineSpec(
                LEVEL,
                "Legacy control account " + control.account() + " = open legacy invoices",
                currency,
                ReconLine.Values.of(expected, expected, posted),
                "GL balance of " + control.account() + " against the open detail of the ledger"));
      }
    }
    return out;
  }

  private static RowCallbackHandler into(Map<String, BigDecimal> out) {
    return rs -> out.put(rs.getString("currency"), rs.getBigDecimal("amount"));
  }

  /**
   * A legacy control account.
   *
   * @param account account code
   * @param sign 1 for a debit balance, -1 for a credit balance
   * @param components ledger components (one to three)
   */
  private record Control(String account, int sign, List<String> components) {}
}
