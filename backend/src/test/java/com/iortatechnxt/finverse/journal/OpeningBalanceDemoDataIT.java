package com.iortatechnxt.finverse.journal;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.journal.demo.OpeningBalanceDemoData;
import com.iortatechnxt.finverse.receivables.domain.BankReconciliation;
import com.iortatechnxt.finverse.receivables.domain.BankStatement;
import com.iortatechnxt.finverse.receivables.domain.BankStatementLine;
import com.iortatechnxt.finverse.receivables.domain.ReconciliationStatus;
import com.iortatechnxt.finverse.receivables.service.BankReconciliationService;
import com.iortatechnxt.finverse.receivables.service.BankStatementService;
import com.iortatechnxt.finverse.support.TestData;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Loads the demo profile in its own database (the context and database are shared with the other
 * demo data tests) and checks the bank opening balances: one OPENING journal per branch on 1
 * January 2026, posted once; every bank account of the demo company in credit at every month end of
 * 2026 after all demo activity, for the company and for each branch; and the demo bank
 * reconciliation still finalized, the opening balance matched as the balance brought forward.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "demo"})
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
class OpeningBalanceDemoDataIT {

  private static final YearMonth FIRST_MONTH = YearMonth.of(2026, 1);
  private static final YearMonth LAST_MONTH = YearMonth.of(2026, 9);
  private static final String BDO = "1111";
  private static final String BPI = "1112";

  @Autowired private OpeningBalanceDemoData loader;
  @Autowired private BankStatementService statements;
  @Autowired private BankReconciliationService reconciliation;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TestData data;

  private Long company() {
    return data.company().getId();
  }

  private int openingJournals() {
    return jdbc.queryForObject(
        """
        select count(*) from jnl_batch
        where company_id = ? and source_module = ? and source_reference like ?
          and journal_type = 'OPENING' and status = 'POSTED' and value_date = ?
        """,
        Integer.class,
        company(),
        OpeningBalanceDemoData.SOURCE,
        OpeningBalanceDemoData.REFERENCE + "-%",
        Date.valueOf(OpeningBalanceDemoData.TAKE_ON));
  }

  private BigDecimal balance(String account, LocalDate asOf) {
    return jdbc.queryForObject(
        """
        select coalesce(sum(e.debit_fc - e.credit_fc), 0) from gl_ledger_entry e
        join coa_account a on a.id = e.account_id
        where e.company_id = ? and a.code = ? and e.value_date <= ?
        """,
        BigDecimal.class,
        company(),
        account,
        Date.valueOf(asOf));
  }

  private List<Map<String, Object>> branchBalances(LocalDate asOf) {
    return jdbc.queryForList(
        """
        select a.code as account, b.code as branch, sum(e.debit_fc - e.credit_fc) as balance
        from gl_ledger_entry e join coa_account a on a.id = e.account_id
        join org_branch b on b.id = e.branch_id
        where e.company_id = ? and a.code in (?, ?) and e.value_date <= ?
        group by a.code, b.code
        """,
        company(),
        BDO,
        BPI,
        Date.valueOf(asOf));
  }

  @Test
  void openingJournalsArePostedOnceAndKeepEveryBankAccountInCredit() {
    assertThat(openingJournals()).isEqualTo(3);
    loader.run(null);
    assertThat(openingJournals()).isEqualTo(3);
    assertThat(balance(BDO, OpeningBalanceDemoData.TAKE_ON)).isEqualByComparingTo("150000000.00");
    assertThat(balance(BPI, OpeningBalanceDemoData.TAKE_ON)).isEqualByComparingTo("20000000.00");

    List<String> banks =
        jdbc.queryForList(
            """
            select a.code from coa_account a join coa_category c on c.id = a.category_id
            where a.company_id = ? and a.postable and c.bank_category order by a.code
            """,
            String.class,
            company());
    assertThat(banks).contains(BDO, BPI, "1113");
    for (YearMonth m = FIRST_MONTH; !m.isAfter(LAST_MONTH); m = m.plusMonths(1)) {
      LocalDate monthEnd = m.atEndOfMonth();
      for (String bank : banks) {
        assertThat(balance(bank, monthEnd)).as("%s at %s", bank, monthEnd).isNotNegative();
      }
      assertThat(balance(BDO, monthEnd)).as("1111 at %s", monthEnd).isPositive();
      assertThat(balance(BPI, monthEnd)).as("1112 at %s", monthEnd).isPositive();
      assertThat(branchBalances(monthEnd))
          .as("branch balances of 1111 and 1112 at %s", monthEnd)
          .hasSize(4)
          .allSatisfy(r -> assertThat((BigDecimal) r.get("balance")).isPositive());
    }
  }

  @Test
  void bankStatementBringsTheOpeningBalanceForwardAndStillReconciles() {
    BankStatement january =
        statements.list(company(), BDO).stream()
            .min(Comparator.comparing(BankStatement::getId))
            .orElseThrow();
    BankStatementLine first = statements.lines(january.getId()).get(0);
    assertThat(first.getDescription()).isEqualTo("BALANCE BROUGHT FORWARD");
    assertThat(first.getValueDate()).isEqualTo(OpeningBalanceDemoData.TAKE_ON);
    assertThat(first.getCredit()).isEqualByComparingTo("150000000.00");
    assertThat(first.getMatchId()).isNotNull();

    List<BankReconciliation> recs = reconciliation.list(company());
    assertThat(recs)
        .anySatisfy(
            r -> {
              assertThat(r.getAsOfDate()).isEqualTo(LocalDate.of(2026, 8, 31));
              assertThat(r.getStatus()).isEqualTo(ReconciliationStatus.FINALIZED);
            });
    assertThat(reconciliation.statement(company(), BDO, LocalDate.of(2026, 9, 19)).figures())
        .satisfies(f -> assertThat(f.difference()).isZero());
  }
}
