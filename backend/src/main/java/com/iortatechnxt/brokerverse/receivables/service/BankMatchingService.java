package com.iortatechnxt.brokerverse.receivables.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.receivables.api.dto.AutoMatchRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.ManualMatchRequest;
import com.iortatechnxt.brokerverse.receivables.domain.BankMatch;
import com.iortatechnxt.brokerverse.receivables.domain.BankMatchBook;
import com.iortatechnxt.brokerverse.receivables.domain.BankMatchBookRepository;
import com.iortatechnxt.brokerverse.receivables.domain.BankMatchRepository;
import com.iortatechnxt.brokerverse.receivables.domain.BankReconciliation;
import com.iortatechnxt.brokerverse.receivables.domain.BankReconciliationRepository;
import com.iortatechnxt.brokerverse.receivables.domain.BankStatementLine;
import com.iortatechnxt.brokerverse.receivables.domain.BankStatementLineRepository;
import com.iortatechnxt.brokerverse.receivables.domain.DepositSlip;
import com.iortatechnxt.brokerverse.receivables.domain.DepositSlipRepository;
import com.iortatechnxt.brokerverse.receivables.domain.DepositSlipStatus;
import com.iortatechnxt.brokerverse.receivables.domain.MatchMethod;
import com.iortatechnxt.brokerverse.receivables.domain.Receipt;
import com.iortatechnxt.brokerverse.receivables.domain.ReceiptRepository;
import com.iortatechnxt.brokerverse.receivables.domain.ReconciliationStatus;
import com.iortatechnxt.brokerverse.receivables.service.AutoMatcher.Item;
import com.iortatechnxt.brokerverse.receivables.service.AutoMatcher.Proposal;
import com.iortatechnxt.brokerverse.receivables.service.BankAccountDirectory.BankAccount;
import com.iortatechnxt.brokerverse.receivables.service.BankBookQueries.BookEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Matching of book entries (ledger entries of a bank GL account) with bank statement lines:
 * automatic matching, manual matching and unmatching.
 *
 * <p>A match is dated with the latest date of its items (never on or before the last finalized
 * reconciliation), so reports "as of" an earlier date still see the items as reconciling items and
 * a finalized Bank Reconciliation Statement never changes.
 */
@Service
@Transactional
public class BankMatchingService {

  private static final String ENTITY = "BankMatch";
  private static final int DEFAULT_WINDOW = 7;

  private final BankMatchRepository matches;
  private final BankMatchBookRepository matchBooks;
  private final BankStatementLineRepository lines;
  private final BankReconciliationRepository reconciliations;
  private final DepositSlipRepository slips;
  private final ReceiptRepository receipts;
  private final BankBookQueries book;
  private final BankAccountDirectory banks;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param matches match repository
   * @param matchBooks match book repository
   * @param lines statement line repository
   * @param reconciliations reconciliation repository
   * @param slips deposit slip repository
   * @param receipts receipt repository
   * @param book book queries
   * @param banks bank account directory
   * @param audit audit trail
   */
  public BankMatchingService(
      BankMatchRepository matches,
      BankMatchBookRepository matchBooks,
      BankStatementLineRepository lines,
      BankReconciliationRepository reconciliations,
      DepositSlipRepository slips,
      ReceiptRepository receipts,
      BankBookQueries book,
      BankAccountDirectory banks,
      AuditTrailService audit) {
    this.matches = matches;
    this.matchBooks = matchBooks;
    this.lines = lines;
    this.reconciliations = reconciliations;
    this.slips = slips;
    this.receipts = receipts;
    this.book = book;
    this.banks = banks;
    this.audit = audit;
  }

  /**
   * Unmatched items of a bank account (matching workbench).
   *
   * @param companyId company
   * @param bankAccountCode bank GL account
   * @param asOf items dated on or before
   * @return book entries and bank lines not yet matched
   */
  @Transactional(readOnly = true)
  public Workbench workbench(Long companyId, String bankAccountCode, LocalDate asOf) {
    BankAccount bank = banks.require(companyId, bankAccountCode, null);
    return new Workbench(
        bank,
        book.unmatched(companyId, bank.id(), asOf),
        lines.unmatched(companyId, bankAccountCode, asOf));
  }

  /**
   * Matches automatically on amount, date window and reference, then deposit slips.
   *
   * @param r request
   * @return matches created
   */
  public List<BankMatch> autoMatch(AutoMatchRequest r) {
    Workbench w = workbench(r.companyId(), r.bankAccountCode(), r.asOf());
    Map<Long, BookEntry> bookById =
        w.bookEntries().stream().collect(Collectors.toMap(BookEntry::id, e -> e));
    Map<Long, BankStatementLine> lineById =
        w.bankLines().stream().collect(Collectors.toMap(BankStatementLine::getId, l -> l));
    List<Proposal> proposals =
        AutoMatcher.match(
            w.bookEntries().stream()
                .map(
                    e ->
                        new Item(
                            e.id(), e.valueDate(), e.signedAmount(), e.reference(), e.narration()))
                .toList(),
            w.bankLines().stream()
                .map(
                    l ->
                        new Item(
                            l.getId(),
                            l.getValueDate(),
                            l.signedAmount(),
                            l.getReference(),
                            l.getDescription()))
                .toList(),
            r.dateWindowDays() == null ? DEFAULT_WINDOW : r.dateWindowDays(),
            slipReceipts(r.companyId(), r.bankAccountCode()));
    return proposals.stream()
        .map(
            p ->
                create(
                    r.companyId(),
                    r.bankAccountCode(),
                    MatchMethod.AUTO,
                    p.bookIds().stream().map(bookById::get).toList(),
                    p.bankIds().stream().map(lineById::get).toList()))
        .toList();
  }

  /**
   * Matches selected book entries with selected bank lines of the same total, or offsetting entries
   * of one side only that net to zero (a bounced cheque and its reversal).
   *
   * @param r request
   * @return match
   */
  public BankMatch manualMatch(ManualMatchRequest r) {
    BankAccount bank = banks.require(r.companyId(), r.bankAccountCode(), null);
    Set<Long> bookIds = new HashSet<>(r.ledgerEntryIds());
    List<BookEntry> entries =
        book.unreconciledByIds(r.companyId(), bank.id(), List.copyOf(bookIds));
    if (entries.size() != bookIds.size()) {
      throw new BusinessRuleException(
          "INVALID_MATCH", "Book entries must be unreconciled entries of " + bank.code());
    }
    List<BankStatementLine> selected = lines.findByIdIn(new HashSet<>(r.statementLineIds()));
    boolean valid =
        selected.size() == new HashSet<>(r.statementLineIds()).size()
            && selected.stream()
                .allMatch(
                    l ->
                        l.getMatchId() == null
                            && l.getCompanyId().equals(r.companyId())
                            && l.getBankAccountCode().equals(bank.code()));
    if (!valid) {
      throw new BusinessRuleException(
          "INVALID_MATCH", "Bank lines must be unreconciled lines of " + bank.code());
    }
    return create(r.companyId(), bank.code(), MatchMethod.MANUAL, entries, selected);
  }

  /**
   * Undoes a match (not allowed once covered by a finalized reconciliation).
   *
   * @param matchId match
   * @return removed match
   */
  public BankMatch unmatch(Long matchId) {
    BankMatch match =
        matches.findById(matchId).orElseThrow(() -> new ResourceNotFoundException(ENTITY, matchId));
    lastFinalized(match.getCompanyId(), match.getBankAccountCode())
        .filter(d -> !match.getMatchDate().isAfter(d))
        .ifPresent(
            d -> {
              throw new BusinessRuleException(
                  "RECONCILIATION_FINALIZED",
                  "The match is part of the reconciliation finalized as of " + d);
            });
    lines.findByMatchId(matchId).forEach(BankStatementLine::unmatch);
    matchBooks.deleteAll(matchBooks.findByMatchId(matchId));
    matches.delete(match);
    audit.record(ENTITY, matchId, AuditAction.REVERSE, "Unmatched " + match.getAmount());
    return match;
  }

  /**
   * Lists the latest matches of a bank account.
   *
   * @param companyId company
   * @param bankAccountCode bank GL account
   * @return matches newest first
   */
  @Transactional(readOnly = true)
  public List<BankMatch> recent(Long companyId, String bankAccountCode) {
    return matches.findTop200ByCompanyIdAndBankAccountCodeOrderByIdDesc(companyId, bankAccountCode);
  }

  private BankMatch create(
      Long companyId,
      String bankAccountCode,
      MatchMethod method,
      List<BookEntry> entries,
      List<BankStatementLine> bankLines) {
    if (entries.size() + bankLines.size() < 2) {
      throw new BusinessRuleException(
          "INVALID_MATCH",
          "Select book entries and bank lines of the same total, or offsetting items of one side");
    }
    BigDecimal bookTotal =
        entries.stream().map(BookEntry::signedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal bankTotal =
        bankLines.stream()
            .map(BankStatementLine::signedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (bookTotal.compareTo(bankTotal) != 0) {
      throw new BusinessRuleException(
          "MATCH_NOT_BALANCED",
          "Book total " + bookTotal + " differs from bank total " + bankTotal);
    }
    LocalDate date =
        Stream.concat(
                entries.stream().map(BookEntry::valueDate),
                bankLines.stream().map(BankStatementLine::getValueDate))
            .max(LocalDate::compareTo)
            .orElseThrow();
    Optional<LocalDate> finalized = lastFinalized(companyId, bankAccountCode);
    if (finalized.isPresent() && !date.isAfter(finalized.get())) {
      date = finalized.get().plusDays(1);
    }
    BankMatch match =
        matches.save(new BankMatch(companyId, bankAccountCode, method, date, bookTotal));
    matchBooks.saveAll(
        entries.stream()
            .map(e -> new BankMatchBook(match.getId(), e.id(), e.signedAmount()))
            .toList());
    bankLines.forEach(l -> l.match(match.getId()));
    audit.record(
        ENTITY,
        match.getId(),
        AuditAction.CREATE,
        method + " match of " + bookTotal + " on " + bankAccountCode);
    return match;
  }

  private Optional<LocalDate> lastFinalized(Long companyId, String bankAccountCode) {
    return reconciliations
        .findFirstByCompanyIdAndBankAccountCodeAndStatusOrderByAsOfDateDesc(
            companyId, bankAccountCode, ReconciliationStatus.FINALIZED)
        .map(BankReconciliation::getAsOfDate);
  }

  private Map<String, Set<String>> slipReceipts(Long companyId, String bankAccountCode) {
    Map<String, Set<String>> result = new HashMap<>();
    for (DepositSlip slip :
        slips.findByCompanyIdAndBankAccountCodeAndStatus(
            companyId, bankAccountCode, DepositSlipStatus.DEPOSITED)) {
      result.put(
          slip.getSlipNo(),
          receipts.findByDepositSlipIdOrderByIdAsc(slip.getId()).stream()
              .map(Receipt::getReceiptNo)
              .collect(Collectors.toSet()));
    }
    return result;
  }

  /**
   * Unmatched items of a bank account.
   *
   * @param bank bank account
   * @param bookEntries unmatched book entries
   * @param bankLines unmatched bank lines
   */
  public record Workbench(
      BankAccount bank, List<BookEntry> bookEntries, List<BankStatementLine> bankLines) {}
}
