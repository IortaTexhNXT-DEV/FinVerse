package com.iortatechnxt.brokerverse.receivables.report;

import com.iortatechnxt.brokerverse.receivables.domain.BankStatementLine;
import com.iortatechnxt.brokerverse.receivables.service.BankAccountDirectory;
import com.iortatechnxt.brokerverse.receivables.service.BankReconciliationService;
import com.iortatechnxt.brokerverse.receivables.service.BankReconciliationService.Brs;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * FIN-BRS-UNREC-BANK (Src BR002) Un-reconciled Bank Entries: bank statement lines not matched to
 * the books as of a date (bank charges, direct credits, returned cheques not yet accounted).
 */
@Component
public class UnreconciledBankEntriesReport extends AbstractUnreconciledReport {

  /**
   * Creates the report.
   *
   * @param reconciliation reconciliation service
   * @param banks bank account directory
   */
  public UnreconciledBankEntriesReport(
      BankReconciliationService reconciliation, BankAccountDirectory banks) {
    super(reconciliation, banks, "FIN-BRS-UNREC-BANK", "Un-reconciled Bank Entries");
  }

  @Override
  protected List<Item> items(Brs brs) {
    String currency = brs.bank().currency();
    return Stream.concat(brs.bankDebits().stream(), brs.bankCredits().stream())
        .sorted(
            Comparator.comparing(BankStatementLine::getValueDate)
                .thenComparing(BankStatementLine::getId))
        .map(
            l ->
                new Item(
                    l.getValueDate(),
                    l.getDescription(),
                    "Line " + l.getLineNo(),
                    l.getReference(),
                    l.getDebit(),
                    l.getCredit(),
                    currency))
        .toList();
  }
}
