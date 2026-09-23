package com.iortatechnxt.finverse.receivables.report;

import com.iortatechnxt.finverse.receivables.service.BankAccountDirectory;
import com.iortatechnxt.finverse.receivables.service.BankBookQueries.BookEntry;
import com.iortatechnxt.finverse.receivables.service.BankReconciliationService;
import com.iortatechnxt.finverse.receivables.service.BankReconciliationService.Brs;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * FIN-BRS-UNREC-BOOK (Src BR001) Un-reconciled Book Entries: ledger entries of bank accounts not
 * matched to the bank statement as of a date (deposits in transit, unpresented cheques).
 */
@Component
public class UnreconciledBookEntriesReport extends AbstractUnreconciledReport {

  /**
   * Creates the report.
   *
   * @param reconciliation reconciliation service
   * @param banks bank account directory
   */
  public UnreconciledBookEntriesReport(
      BankReconciliationService reconciliation, BankAccountDirectory banks) {
    super(reconciliation, banks, "FIN-BRS-UNREC-BOOK", "Un-reconciled Book Entries");
  }

  @Override
  protected List<Item> items(Brs brs) {
    return Stream.concat(brs.bookDebits().stream(), brs.bookCredits().stream())
        .sorted(Comparator.comparing(BookEntry::valueDate).thenComparing(BookEntry::id))
        .map(
            e ->
                new Item(
                    e.valueDate(),
                    e.narration(),
                    e.batchNo(),
                    e.reference(),
                    e.debit(),
                    e.credit(),
                    e.currency()))
        .toList();
  }
}
