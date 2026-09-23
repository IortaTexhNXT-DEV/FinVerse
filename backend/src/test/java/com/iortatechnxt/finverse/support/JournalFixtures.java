package com.iortatechnxt.finverse.support;

import com.iortatechnxt.finverse.coa.domain.BalanceSide;
import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.finverse.journal.api.dto.JournalRequest;
import com.iortatechnxt.finverse.journal.domain.JournalBatch;
import com.iortatechnxt.finverse.journal.domain.JournalType;
import com.iortatechnxt.finverse.journal.service.JournalAuthorizationService;
import com.iortatechnxt.finverse.journal.service.JournalEntryService;
import com.iortatechnxt.finverse.ledger.service.LedgerQueryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/** Creates manual journals in the demo company for platform feature tests. */
@Component
public class JournalFixtures {

  private final JournalEntryService entries;
  private final JournalAuthorizationService authorization;
  private final LedgerQueryService ledger;
  private final ChartOfAccountsService accounts;
  private final AsUser as;
  private final TestData data;

  JournalFixtures(
      JournalEntryService entries,
      JournalAuthorizationService authorization,
      LedgerQueryService ledger,
      ChartOfAccountsService accounts,
      AsUser as,
      TestData data) {
    this.entries = entries;
    this.authorization = authorization;
    this.ledger = ledger;
    this.accounts = accounts;
    this.as = as;
    this.data = data;
  }

  /** Net base-currency balance (debit positive) of a demo company account, all branches. */
  public BigDecimal balance(String accountCode, LocalDate asOf) {
    Long company = data.company().getId();
    return ledger.netBalance(company, accounts.getByCode(company, accountCode).getId(), null, asOf);
  }

  /** Two-line journal request (the debit account may need cost centre FIN). */
  public JournalRequest request(
      String debitAccount, String creditAccount, String amount, LocalDate valueDate) {
    BigDecimal value = new BigDecimal(amount);
    return new JournalRequest(
        data.company().getId(),
        data.branch("HO").getId(),
        JournalType.MANUAL,
        valueDate,
        "PHP",
        "Platform feature test journal",
        "PF-TEST",
        List.of(
            line(debitAccount, BalanceSide.DEBIT, value),
            line(creditAccount, BalanceSide.CREDIT, value)));
  }

  /** Draft created and submitted by the accountant. */
  public JournalBatch submitted(JournalRequest request) {
    JournalBatch draft = as.run("accountant", () -> entries.createDraft(request));
    return as.run("accountant", () -> entries.submit(draft.getId()));
  }

  /** Journal created by the accountant and approved (posted) by the finance manager. */
  public JournalBatch posted(JournalRequest request) {
    JournalBatch batch = submitted(request);
    return as.run("fmanager", () -> authorization.approve(batch.getId()));
  }

  private static JournalLineRequest line(String account, BalanceSide side, BigDecimal amount) {
    return new JournalLineRequest(
        account, side, amount, null, null, null, "FIN", null, null, null, null);
  }
}
