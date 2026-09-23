package com.iortatechnxt.finverse.receivables.demo;

import com.iortatechnxt.finverse.receivables.api.dto.AutoMatchRequest;
import com.iortatechnxt.finverse.receivables.api.dto.ReconciliationRequest;
import com.iortatechnxt.finverse.receivables.api.dto.StatementImportRequest;
import com.iortatechnxt.finverse.receivables.domain.BankReconciliation;
import com.iortatechnxt.finverse.receivables.domain.DepositSlip;
import com.iortatechnxt.finverse.receivables.domain.DepositSlipStatus;
import com.iortatechnxt.finverse.receivables.domain.Receipt;
import com.iortatechnxt.finverse.receivables.service.BankAccountDirectory;
import com.iortatechnxt.finverse.receivables.service.BankBookQueries;
import com.iortatechnxt.finverse.receivables.service.BankBookQueries.BookEntry;
import com.iortatechnxt.finverse.receivables.service.BankMatchingService;
import com.iortatechnxt.finverse.receivables.service.BankReconciliationService;
import com.iortatechnxt.finverse.receivables.service.BankStatementService;
import com.iortatechnxt.finverse.receivables.service.DepositService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Demo bank statements of the main bank account (1111) built from its book entries the way a bank
 * reports them (one credit per deposit slip, one line per transfer, cheques paid a few days after
 * issue): January-August, reconciled and finalized as of 31 August, and September to date with bank
 * charges, interest and an unpresented cheque, reconciled but not finalized.
 */
@Component
@Profile("demo")
public class DemoBankStatements {

  /** End of the finalized period. */
  static final LocalDate AUGUST_END = LocalDate.of(2026, 8, 31);

  /** Last date of the September statement. */
  static final LocalDate SEPTEMBER_CUTOFF = LocalDate.of(2026, 9, 19);

  private static final LocalDate SEPTEMBER_START = LocalDate.of(2026, 9, 1);
  private static final LocalDate SERVICE_CHARGE_DATE = LocalDate.of(2026, 9, 15);
  private static final int PRESENTATION_DAYS = 2;
  private static final int WINDOW = 7;
  private static final String HEADER = "date,description,reference,debit,credit,balance\n";
  private static final String RECEIPT_PREFIX = "OR-";

  private final BankAccountDirectory banks;
  private final BankBookQueries book;
  private final DepositService deposits;
  private final BankStatementService statements;
  private final BankMatchingService matching;
  private final BankReconciliationService reconciliation;

  /**
   * Creates the builder.
   *
   * @param banks bank account directory
   * @param book book queries
   * @param deposits deposit service
   * @param statements statement service
   * @param matching matching service
   * @param reconciliation reconciliation service
   */
  public DemoBankStatements(
      BankAccountDirectory banks,
      BankBookQueries book,
      DepositService deposits,
      BankStatementService statements,
      BankMatchingService matching,
      BankReconciliationService reconciliation) {
    this.banks = banks;
    this.book = book;
    this.deposits = deposits;
    this.statements = statements;
    this.matching = matching;
    this.reconciliation = reconciliation;
  }

  /**
   * Imports both statements, matches them and reconciles.
   *
   * @param ctx demo context
   * @return the finalized August reconciliation
   */
  public BankReconciliation run(DemoContext ctx) {
    return DemoContext.as(DemoContext.MANAGER, () -> reconcile(ctx));
  }

  private BankReconciliation reconcile(DemoContext ctx) {
    Long accountId = banks.require(ctx.companyId(), DemoContext.BANK, null).id();
    List<Line> lines = bankLines(ctx, book.unmatched(ctx.companyId(), accountId, SEPTEMBER_CUTOFF));
    List<Line> august = lines.stream().filter(l -> !l.date().isAfter(AUGUST_END)).toList();
    String unpresented = String.valueOf(DemoBanking.RENT_CHEQUE + SEPTEMBER_START.getMonthValue());
    List<Line> september =
        new ArrayList<>(
            lines.stream()
                .filter(l -> l.date().isAfter(AUGUST_END))
                .filter(l -> !unpresented.equals(l.reference()))
                .toList());
    september.add(
        new Line(SERVICE_CHARGE_DATE, "SERVICE CHARGE", "SC-0915", new BigDecimal("-350.00")));
    september.add(
        new Line(SEPTEMBER_CUTOFF, "INTEREST CREDIT", "INT-0919", new BigDecimal("1250.37")));
    september.sort(Comparator.comparing(Line::date));
    BigDecimal closing = importStatement(ctx, "BDO-1111-2026-01-08", august, BigDecimal.ZERO);
    matching.autoMatch(new AutoMatchRequest(ctx.companyId(), DemoContext.BANK, AUGUST_END, WINDOW));
    BankReconciliation finalized =
        reconciliation.finalizeReconciliation(
            reconciliation
                .save(new ReconciliationRequest(ctx.companyId(), DemoContext.BANK, AUGUST_END))
                .getId());
    importStatement(ctx, "BDO-1111-2026-09", september, closing);
    matching.autoMatch(
        new AutoMatchRequest(ctx.companyId(), DemoContext.BANK, DemoContext.LAST_DATE, WINDOW));
    reconciliation.save(
        new ReconciliationRequest(ctx.companyId(), DemoContext.BANK, SEPTEMBER_CUTOFF));
    return finalized;
  }

  private BigDecimal importStatement(
      DemoContext ctx, String ref, List<Line> lines, BigDecimal opening) {
    StringBuilder csv = new StringBuilder(HEADER);
    BigDecimal balance = opening;
    for (Line l : lines) {
      balance = balance.add(l.amount());
      csv.append(l.date())
          .append(',')
          .append(l.description())
          .append(',')
          .append(l.reference() == null ? "" : l.reference())
          .append(',')
          .append(l.amount().signum() < 0 ? l.amount().negate().toPlainString() : "")
          .append(',')
          .append(l.amount().signum() > 0 ? l.amount().toPlainString() : "")
          .append(',')
          .append(balance.toPlainString())
          .append('\n');
    }
    statements.importStatement(
        new StatementImportRequest(
            ctx.companyId(), DemoContext.BANK, ref, ref + ".csv", csv.toString(), opening));
    return balance;
  }

  /**
   * Turns book entries into bank lines: receipts on a deposited slip become one credit per slip,
   * split postings of one receipt become one line, everything else one line each; cheques paid
   * appear a few days after issue.
   */
  private List<Line> bankLines(DemoContext ctx, List<BookEntry> entries) {
    Map<String, DepositSlip> slipOf = slipsByReceipt(ctx);
    Set<String> inTransit =
        deposits.undeposited(ctx.companyId(), DemoContext.BANK).stream()
            .map(Receipt::getReceiptNo)
            .collect(Collectors.toSet());
    Map<String, Line> grouped = new LinkedHashMap<>();
    for (BookEntry e : entries) {
      if (e.reference() == null || !inTransit.contains(e.reference())) {
        Keyed k = toLine(e, slipOf);
        grouped.merge(k.key(), k.line(), Line::plus);
      }
    }
    List<Line> lines = new ArrayList<>(grouped.values());
    lines.sort(Comparator.comparing(Line::date));
    return lines;
  }

  private static Keyed toLine(BookEntry e, Map<String, DepositSlip> slipOf) {
    BigDecimal amount = e.signedAmount();
    String ref = e.reference();
    boolean receiptRef = ref != null && ref.startsWith(RECEIPT_PREFIX);
    if (amount.signum() < 0) {
      Line line = new Line(e.valueDate().plusDays(PRESENTATION_DAYS), debitText(ref), ref, amount);
      return new Keyed(receiptRef ? "N" + ref : "E" + e.id(), line);
    }
    DepositSlip slip = receiptRef ? slipOf.get(ref) : null;
    if (slip != null) {
      String no = slip.getSlipNo();
      return new Keyed(no, new Line(slip.getDepositedOn(), "DEPOSIT " + no, no, amount));
    }
    return receiptRef
        ? new Keyed(ref, new Line(e.valueDate(), "INWARD CREDIT " + ref, ref, amount))
        : new Keyed("E" + e.id(), new Line(e.valueDate(), "CREDIT", ref, amount));
  }

  private static String debitText(String ref) {
    return ref != null && ref.startsWith(RECEIPT_PREFIX) ? "RETURNED CHEQUE " + ref : "CHEQUE PAID";
  }

  private Map<String, DepositSlip> slipsByReceipt(DemoContext ctx) {
    Map<String, DepositSlip> result = new HashMap<>();
    for (DepositSlip slip : deposits.list(ctx.companyId())) {
      if (slip.getStatus() == DepositSlipStatus.DEPOSITED
          && DemoContext.BANK.equals(slip.getBankAccountCode())) {
        for (Receipt r : deposits.receiptsOf(slip.getId())) {
          result.put(r.getReceiptNo(), slip);
        }
      }
    }
    return result;
  }

  /**
   * Line with its grouping key.
   *
   * @param key grouping key
   * @param line line
   */
  private record Keyed(String key, Line line) {}

  /**
   * Bank statement line being generated.
   *
   * @param date value date
   * @param description description
   * @param reference reference
   * @param amount signed amount (deposits positive)
   */
  private record Line(LocalDate date, String description, String reference, BigDecimal amount) {

    Line plus(Line other) {
      return new Line(
          date.isAfter(other.date()) ? date : other.date(),
          description,
          reference,
          amount.add(other.amount()));
    }
  }
}
