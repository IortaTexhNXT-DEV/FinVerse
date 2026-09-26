package com.iortatechnxt.brokerverse.receivables.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.receivables.api.dto.ReconciliationRequest;
import com.iortatechnxt.brokerverse.receivables.domain.BankReconciliation;
import com.iortatechnxt.brokerverse.receivables.domain.BankReconciliationRepository;
import com.iortatechnxt.brokerverse.receivables.domain.BankStatementLine;
import com.iortatechnxt.brokerverse.receivables.domain.BankStatementLineRepository;
import com.iortatechnxt.brokerverse.receivables.domain.BrsFigures;
import com.iortatechnxt.brokerverse.receivables.service.BankAccountDirectory.BankAccount;
import com.iortatechnxt.brokerverse.receivables.service.BankBookQueries.BookEntry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bank Reconciliation Statement (BRS) of a GL bank account as of a date, and the saved / finalized
 * reconciliations.
 *
 * <p>Items not reconciled as of the date are the reconciling items: book debits (deposits in
 * transit), book credits (unpresented cheques), bank debits (charges, returned cheques) and bank
 * credits (interest, direct credits) not yet in the book. See {@link BrsFigures} for the formula.
 */
@Service
@Transactional
public class BankReconciliationService {

  private static final String ENTITY = "BankReconciliation";

  private final BankReconciliationRepository reconciliations;
  private final BankStatementLineRepository lines;
  private final BankBookQueries book;
  private final BankAccountDirectory banks;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param reconciliations reconciliation repository
   * @param lines statement lines
   * @param book book queries
   * @param banks bank account directory
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public BankReconciliationService(
      BankReconciliationRepository reconciliations,
      BankStatementLineRepository lines,
      BankBookQueries book,
      BankAccountDirectory banks,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.reconciliations = reconciliations;
    this.lines = lines;
    this.book = book;
    this.banks = banks;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Computes the Bank Reconciliation Statement.
   *
   * @param companyId company
   * @param bankAccountCode GL bank account
   * @param asOf statement date
   * @return statement with its reconciling items
   */
  @Transactional(readOnly = true)
  public Brs statement(Long companyId, String bankAccountCode, LocalDate asOf) {
    BankAccount bank = banks.require(companyId, bankAccountCode, null);
    List<BookEntry> bookItems = book.unreconciled(companyId, bank.id(), asOf);
    List<BankStatementLine> bankItems = lines.unreconciled(companyId, bankAccountCode, asOf);
    List<BookEntry> bookDebits =
        bookItems.stream().filter(e -> e.signedAmount().signum() > 0).toList();
    List<BookEntry> bookCredits =
        bookItems.stream().filter(e -> e.signedAmount().signum() < 0).toList();
    List<BankStatementLine> bankDebits =
        bankItems.stream().filter(l -> l.getDebit().signum() > 0).toList();
    List<BankStatementLine> bankCredits =
        bankItems.stream().filter(l -> l.getCredit().signum() > 0).toList();
    Optional<BankStatementLine> last = lines.latestOnOrBefore(companyId, bankAccountCode, asOf);
    BrsFigures figures =
        BrsFigures.of(
            Money.round(book.balance(companyId, bank.id(), asOf)),
            sum(bookDebits, BookEntry::debit),
            sum(bookCredits, BookEntry::credit),
            sum(bankDebits, BankStatementLine::getDebit),
            sum(bankCredits, BankStatementLine::getCredit),
            last.map(BankStatementLine::getBalance).orElse(Money.zero()));
    return new Brs(
        bank,
        asOf,
        figures,
        last.isPresent(),
        bookDebits,
        bookCredits,
        bankDebits,
        bankCredits,
        reconciliations.findByCompanyIdAndBankAccountCodeAndAsOfDate(
            companyId, bankAccountCode, asOf));
  }

  /**
   * Number of reconciling items of every bank account of a company as of a date: book entries not
   * matched to the bank and bank statement lines not matched to the book, exactly the items of the
   * BRS and of the reports FIN-BRS-UNREC-BOOK / FIN-BRS-UNREC-BANK.
   *
   * @param companyId company
   * @param asOf date
   * @return unreconciled items
   */
  @Transactional(readOnly = true)
  public long unreconciledItems(Long companyId, LocalDate asOf) {
    return banks.list(companyId).stream()
        .mapToLong(b -> statement(companyId, b.code(), asOf).itemCount())
        .sum();
  }

  /**
   * Saves (or refreshes) the reconciliation as of a date with the current BRS figures.
   *
   * @param r request
   * @return reconciliation in progress
   */
  public BankReconciliation save(ReconciliationRequest r) {
    Brs brs = statement(r.companyId(), r.bankAccountCode(), r.asOf());
    BankReconciliation rec;
    if (brs.reconciliation().isPresent()) {
      rec = brs.reconciliation().get();
      rec.refresh(brs.figures());
    } else {
      rec =
          reconciliations.save(
              new BankReconciliation(r.companyId(), r.bankAccountCode(), r.asOf(), brs.figures()));
    }
    audit.record(
        ENTITY,
        r.bankAccountCode() + "@" + r.asOf(),
        AuditAction.UPDATE,
        "Difference " + brs.figures().difference());
    return rec;
  }

  /**
   * Finalizes a reconciliation; requires an imported statement and a zero difference.
   *
   * @param id reconciliation
   * @return finalized reconciliation
   */
  public BankReconciliation finalizeReconciliation(Long id) {
    BankReconciliation rec = get(id);
    Brs brs = statement(rec.getCompanyId(), rec.getBankAccountCode(), rec.getAsOfDate());
    if (!brs.hasStatement()) {
      throw new BusinessRuleException(
          "NO_BANK_STATEMENT", "Import the bank statement up to " + rec.getAsOfDate() + " first");
    }
    rec.refresh(brs.figures());
    rec.finalizeReconciliation(currentUser.username(), clock.instant());
    audit.record(
        ENTITY,
        rec.getBankAccountCode() + "@" + rec.getAsOfDate(),
        AuditAction.CLOSE,
        "Reconciliation finalized");
    return rec;
  }

  /**
   * Gets a reconciliation.
   *
   * @param id id
   * @return reconciliation
   */
  @Transactional(readOnly = true)
  public BankReconciliation get(Long id) {
    return reconciliations
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Lists reconciliations.
   *
   * @param companyId company
   * @return reconciliations latest first
   */
  @Transactional(readOnly = true)
  public List<BankReconciliation> list(Long companyId) {
    return reconciliations.findByCompanyIdOrderByAsOfDateDescBankAccountCodeAsc(companyId);
  }

  private static <T> BigDecimal sum(List<T> items, Function<T, BigDecimal> amount) {
    return items.stream().map(amount).reduce(Money.zero(), BigDecimal::add);
  }

  /**
   * Bank Reconciliation Statement.
   *
   * @param bank bank account
   * @param asOf statement date
   * @param figures balances and totals
   * @param hasStatement whether bank statement lines exist up to the date
   * @param bookDebits (1) book debits not accounted by the bank
   * @param bookCredits (2) book credits not accounted by the bank
   * @param bankDebits (3) bank debits not accounted in the book
   * @param bankCredits (4) bank credits not accounted in the book
   * @param reconciliation saved reconciliation as of the date, if any
   */
  public record Brs(
      BankAccount bank,
      LocalDate asOf,
      BrsFigures figures,
      boolean hasStatement,
      List<BookEntry> bookDebits,
      List<BookEntry> bookCredits,
      List<BankStatementLine> bankDebits,
      List<BankStatementLine> bankCredits,
      Optional<BankReconciliation> reconciliation) {

    /**
     * Number of reconciling items (book and bank side).
     *
     * @return items not reconciled as of the date
     */
    public int itemCount() {
      return bookDebits.size() + bookCredits.size() + bankDebits.size() + bankCredits.size();
    }
  }
}
