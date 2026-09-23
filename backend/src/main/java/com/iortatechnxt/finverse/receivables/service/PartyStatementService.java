package com.iortatechnxt.finverse.receivables.service;

import com.iortatechnxt.finverse.receivables.service.ReceivablesQueries.ArItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Statement of account with matched and unmatched details (Src FR2545): the documents of a party in
 * a period split into those fully knocked off by the end of the period and those still open.
 */
@Service
@Transactional(readOnly = true)
public class PartyStatementService {

  private final ReceivablesQueries queries;

  /**
   * Creates the service.
   *
   * @param queries receivables read model
   */
  public PartyStatementService(ReceivablesQueries queries) {
    this.queries = queries;
  }

  /**
   * Builds the statements of the selected parties.
   *
   * @param companyId company
   * @param from first document date
   * @param to last document date (balances as of this date)
   * @param foreign true for document currency amounts, false for base currency
   * @param filter party selection
   * @return one statement per party with documents in the period
   */
  public List<PartyStatement> statements(
      Long companyId, LocalDate from, LocalDate to, boolean foreign, Predicate<ArItem> filter) {
    Map<String, List<ArItem>> byParty = new LinkedHashMap<>();
    for (ArItem i : queries.items(companyId, from, to, to)) {
      if (filter.test(i)) {
        byParty.computeIfAbsent(i.partyCode(), k -> new ArrayList<>()).add(i);
      }
    }
    List<PartyStatement> result = new ArrayList<>();
    byParty.forEach((code, items) -> result.add(statement(items, foreign)));
    return result;
  }

  private static PartyStatement statement(List<ArItem> items, boolean foreign) {
    List<StatementLine> matched = new ArrayList<>();
    List<StatementLine> unmatched = new ArrayList<>();
    for (ArItem i : items) {
      StatementLine line = line(i, foreign);
      (i.balance().signum() == 0 ? matched : unmatched).add(line);
    }
    ArItem first = items.get(0);
    return new PartyStatement(
        first.partyCode(), first.partyName(), matched, unmatched, net(matched), net(unmatched));
  }

  private static StatementLine line(ArItem i, boolean foreign) {
    BigDecimal original = i.signedOriginal(foreign);
    return new StatementLine(
        i.id(),
        i.documentDate(),
        i.narration(),
        i.documentType() + "-" + i.documentNo(),
        i.chequeNo(),
        i.chequeDate(),
        i.currency(),
        original.signum() > 0 ? original : BigDecimal.ZERO,
        original.signum() < 0 ? original.negate() : BigDecimal.ZERO,
        original,
        i.signedBalance(foreign));
  }

  private static BigDecimal net(List<StatementLine> lines) {
    return lines.stream().map(StatementLine::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Statement of one party.
   *
   * @param partyCode party
   * @param partyName name
   * @param matched documents fully knocked off
   * @param unmatched documents with a balance
   * @param matchedNet net balance of matched documents (zero)
   * @param unmatchedNet net balance of unmatched documents (Dr positive)
   */
  public record PartyStatement(
      String partyCode,
      String partyName,
      List<StatementLine> matched,
      List<StatementLine> unmatched,
      BigDecimal matchedNet,
      BigDecimal unmatchedNet) {}

  /**
   * Statement line.
   *
   * @param itemId open item
   * @param documentDate document date
   * @param reference document reference (narration)
   * @param transactionCode document type and number
   * @param chequeNo cheque number (receipts)
   * @param chequeDate cheque date (receipts)
   * @param currency currency
   * @param debit debit amount
   * @param credit credit amount
   * @param original original amount Dr(+)/Cr(-)
   * @param balance balance Dr(+)/Cr(-) at the end of the period
   */
  public record StatementLine(
      Long itemId,
      LocalDate documentDate,
      String reference,
      String transactionCode,
      String chequeNo,
      LocalDate chequeDate,
      String currency,
      BigDecimal debit,
      BigDecimal credit,
      BigDecimal original,
      BigDecimal balance) {}
}
