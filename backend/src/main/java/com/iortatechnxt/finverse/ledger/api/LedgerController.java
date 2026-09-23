package com.iortatechnxt.finverse.ledger.api;

import com.iortatechnxt.finverse.coa.domain.GlAccount;
import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.ledger.api.dto.AccountStatementResponse;
import com.iortatechnxt.finverse.ledger.api.dto.StatementLineResponse;
import com.iortatechnxt.finverse.ledger.domain.LedgerEntry;
import com.iortatechnxt.finverse.ledger.service.LedgerQueryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** GL inquiry: account statements with running balance and drill-down to journals. */
@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {

  private final LedgerQueryService ledger;
  private final ChartOfAccountsService accounts;

  /**
   * Creates the controller.
   *
   * @param ledger ledger read model
   * @param accounts chart of accounts
   */
  public LedgerController(LedgerQueryService ledger, ChartOfAccountsService accounts) {
    this.ledger = ledger;
    this.accounts = accounts;
  }

  /**
   * Returns the statement of an account for a date range.
   *
   * @param accountId account
   * @param from start date
   * @param to end date
   * @param branchId optional branch
   * @return statement
   */
  @GetMapping("/accounts/{accountId}/statement")
  @PreAuthorize("hasAuthority('JOURNAL_VIEW')")
  public AccountStatementResponse statement(
      @PathVariable Long accountId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) Long branchId) {
    GlAccount account = accounts.get(accountId);
    var statement = ledger.statement(account.getCompanyId(), accountId, branchId, from, to);
    BigDecimal running = statement.openingBalance();
    BigDecimal totalDebit = BigDecimal.ZERO;
    BigDecimal totalCredit = BigDecimal.ZERO;
    List<StatementLineResponse> lines = new ArrayList<>();
    for (LedgerEntry e : statement.entries()) {
      running = running.add(e.getDebitBase()).subtract(e.getCreditBase());
      totalDebit = totalDebit.add(e.getDebitBase());
      totalCredit = totalCredit.add(e.getCreditBase());
      lines.add(
          new StatementLineResponse(
              e.getValueDate(),
              e.getBatchId(),
              e.getBatchNo(),
              e.getJournalType(),
              e.getNarration(),
              e.getReference(),
              e.getCurrency(),
              e.getDebitFc().subtract(e.getCreditFc()),
              e.getDebitBase(),
              e.getCreditBase(),
              running));
    }
    return new AccountStatementResponse(
        accountId,
        account.getCode(),
        account.getName(),
        from,
        to,
        statement.openingBalance(),
        totalDebit,
        totalCredit,
        running,
        lines);
  }
}
