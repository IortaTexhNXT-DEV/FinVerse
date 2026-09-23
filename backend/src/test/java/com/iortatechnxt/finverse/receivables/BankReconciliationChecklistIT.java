package com.iortatechnxt.finverse.receivables;

import static com.iortatechnxt.finverse.support.TestCompanies.line;
import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.closing.service.CheckItem;
import com.iortatechnxt.finverse.closing.service.ClosingChecklistService;
import com.iortatechnxt.finverse.coa.domain.BalanceSide;
import com.iortatechnxt.finverse.period.domain.AccountingPeriod;
import com.iortatechnxt.finverse.period.domain.FiscalYear;
import com.iortatechnxt.finverse.receivables.api.dto.StatementImportRequest;
import com.iortatechnxt.finverse.receivables.service.BankReconciliationService;
import com.iortatechnxt.finverse.receivables.service.BankReconciliationService.Brs;
import com.iortatechnxt.finverse.receivables.service.BankReconciliationStatusProvider;
import com.iortatechnxt.finverse.receivables.service.BankStatementService;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestCompanies;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Unreconciled bank items on the period-end and year-end checklists: a fresh company without bank
 * activity passes the control; once its bank accounts carry unmatched book entries and statement
 * lines up to the period end, the control shows a (non-blocking) warning with the same count as the
 * BRS.
 */
@IntegrationTest
class BankReconciliationChecklistIT {

  private static final String RECONCILIATIONS = "RECONCILIATIONS";
  private static final LocalDate MARCH_END = LocalDate.of(2026, 3, 31);

  @Autowired private TestCompanies companies;
  @Autowired private ClosingChecklistService checklist;
  @Autowired private BankStatementService statements;
  @Autowired private BankReconciliationService reconciliation;
  @Autowired private BankReconciliationStatusProvider provider;
  @Autowired private AsUser as;

  private static CheckItem reconciliationItem(List<CheckItem> items) {
    return items.stream().filter(i -> RECONCILIATIONS.equals(i.code())).findFirst().orElseThrow();
  }

  @Test
  void unmatchedBankItemsTurnTheControlFromOkIntoAWarning() {
    Long companyId = companies.create("TBR" + System.nanoTime() % 100_000, "PHP").getId();
    FiscalYear year = companies.openYear(companyId, 2026);
    AccountingPeriod march = companies.period(companyId, MARCH_END);

    CheckItem before = reconciliationItem(checklist.periodEnd(companyId, march.getId()));
    assertThat(before.passed()).isTrue();
    assertThat(before.blocking()).isFalse();
    assertThat(before.detail())
        .isEqualTo("No unreconciled items up to 2026-03-31: Bank reconciliation");

    // Book side: a transfer between two bank accounts, one entry on each (neither in the bank yet).
    companies.post(
        companyId,
        LocalDate.of(2026, 3, 10),
        "PHP",
        List.of(
            line("1111", BalanceSide.DEBIT, "25000"), line("1112", BalanceSide.CREDIT, "25000")));
    // Bank side: a March service charge not in the book, and an April line after the period end.
    as.run(
        "accountant",
        () ->
            statements.importStatement(
                new StatementImportRequest(
                    companyId,
                    "1111",
                    "TBR-MAR",
                    "march.csv",
                    "Date,Description,Reference,Debit,Credit,Balance\n"
                        + "2026-03-20,Service charge,SC-MAR,150.00,,-150.00\n"
                        + "2026-04-02,Interest,INT-APR,,10.00,-140.00\n",
                    null)));

    List<CheckItem> items = checklist.periodEnd(companyId, march.getId());
    CheckItem after = reconciliationItem(items);
    assertThat(after.passed()).isFalse();
    assertThat(after.blocking()).isFalse();
    assertThat(after.blocks()).isFalse();
    assertThat(after.detail())
        .isEqualTo(
            "3 unreconciled item(s) up to 2026-03-31 (Bank reconciliation 3); "
                + "review them before closing");
    assertThat(reconciliationItem(checklist.yearEnd(companyId, year.getId())).detail())
        .startsWith("4 unreconciled item(s) up to 2026-12-31");

    // Same definition as the BRS and the un-reconciled entries reports.
    Brs brs1111 = reconciliation.statement(companyId, "1111", MARCH_END);
    Brs brs1112 = reconciliation.statement(companyId, "1112", MARCH_END);
    assertThat(brs1111.bookDebits()).hasSize(1);
    assertThat(brs1111.bankDebits()).hasSize(1);
    assertThat(brs1112.bookCredits()).hasSize(1);
    assertThat(brs1111.itemCount() + brs1112.itemCount()).isEqualTo(3);
    assertThat(provider.name()).isEqualTo("Bank reconciliation");
    assertThat(provider.unreconciledItems(companyId, LocalDate.of(2026, 2, 28))).isZero();
    assertThat(provider.unreconciledItems(companyId, LocalDate.of(2026, 4, 30))).isEqualTo(4);
  }
}
