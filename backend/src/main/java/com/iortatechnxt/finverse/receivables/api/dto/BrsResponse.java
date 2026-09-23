package com.iortatechnxt.finverse.receivables.api.dto;

import com.iortatechnxt.finverse.receivables.domain.BankMatch;
import com.iortatechnxt.finverse.receivables.domain.BrsFigures;
import com.iortatechnxt.finverse.receivables.domain.MatchMethod;
import com.iortatechnxt.finverse.receivables.service.BankAccountDirectory.BankAccount;
import com.iortatechnxt.finverse.receivables.service.BankBookQueries.BookEntry;
import com.iortatechnxt.finverse.receivables.service.BankMatchingService.Workbench;
import com.iortatechnxt.finverse.receivables.service.BankReconciliationService.Brs;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Bank Reconciliation Statement with its reconciling items.
 *
 * @param bank bank account
 * @param asOf statement date
 * @param figures balances and totals
 * @param hasStatement whether statement lines exist up to the date
 * @param bookDebits (1) book debits not accounted by the bank
 * @param bookCredits (2) book credits not accounted by the bank
 * @param bankDebits (3) bank debits not accounted in the book
 * @param bankCredits (4) bank credits not accounted in the book
 * @param reconciliation saved reconciliation, if any
 */
public record BrsResponse(
    BankAccount bank,
    LocalDate asOf,
    BrsFigures figures,
    boolean hasStatement,
    List<BookEntry> bookDebits,
    List<BookEntry> bookCredits,
    List<BankStatementLineResponse> bankDebits,
    List<BankStatementLineResponse> bankCredits,
    ReconciliationResponse reconciliation) {

  /**
   * Maps a statement.
   *
   * @param b statement
   * @return response
   */
  public static BrsResponse from(Brs b) {
    return new BrsResponse(
        b.bank(),
        b.asOf(),
        b.figures(),
        b.hasStatement(),
        b.bookDebits(),
        b.bookCredits(),
        b.bankDebits().stream().map(BankStatementLineResponse::from).toList(),
        b.bankCredits().stream().map(BankStatementLineResponse::from).toList(),
        b.reconciliation().map(ReconciliationResponse::from).orElse(null));
  }

  /**
   * Matching workbench: unmatched book entries and bank lines side by side.
   *
   * @param bank bank account
   * @param bookEntries unmatched book entries
   * @param bankLines unmatched bank lines
   */
  public record WorkbenchResponse(
      BankAccount bank, List<BookEntry> bookEntries, List<BankStatementLineResponse> bankLines) {

    /**
     * Maps a workbench.
     *
     * @param w workbench
     * @return response
     */
    public static WorkbenchResponse from(Workbench w) {
      return new WorkbenchResponse(
          w.bank(),
          w.bookEntries(),
          w.bankLines().stream().map(BankStatementLineResponse::from).toList());
    }
  }

  /**
   * Reconciliation match.
   *
   * @param id id
   * @param method auto or manual
   * @param matchDate match date
   * @param amount signed amount
   * @param createdBy user
   */
  public record MatchResponse(
      Long id, MatchMethod method, LocalDate matchDate, BigDecimal amount, String createdBy) {

    /**
     * Maps an entity.
     *
     * @param m match
     * @return response
     */
    public static MatchResponse from(BankMatch m) {
      return new MatchResponse(
          m.getId(), m.getMethod(), m.getMatchDate(), m.getAmount(), m.getCreatedBy());
    }
  }
}
