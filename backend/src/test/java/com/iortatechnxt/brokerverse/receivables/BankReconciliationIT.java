package com.iortatechnxt.brokerverse.receivables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.receivables.api.dto.AutoMatchRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.ManualMatchRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.ReconciliationRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.StatementImportRequest;
import com.iortatechnxt.brokerverse.receivables.domain.AllocationMethod;
import com.iortatechnxt.brokerverse.receivables.domain.BankMatch;
import com.iortatechnxt.brokerverse.receivables.domain.BankStatement;
import com.iortatechnxt.brokerverse.receivables.domain.BankStatementLine;
import com.iortatechnxt.brokerverse.receivables.domain.PayerType;
import com.iortatechnxt.brokerverse.receivables.domain.Receipt;
import com.iortatechnxt.brokerverse.receivables.domain.ReceiptMode;
import com.iortatechnxt.brokerverse.receivables.domain.ReconciliationStatus;
import com.iortatechnxt.brokerverse.receivables.service.BankBookQueries.BookEntry;
import com.iortatechnxt.brokerverse.receivables.service.BankMatchingService;
import com.iortatechnxt.brokerverse.receivables.service.BankReconciliationService;
import com.iortatechnxt.brokerverse.receivables.service.BankStatementService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Statement import, automatic and manual matching, Bank Reconciliation Statement arithmetic and
 * finalization, on account 1101 (used by no other test).
 */
@IntegrationTest
class BankReconciliationIT {

  private static final String CASH = "1101";
  private static final LocalDate JUNE_END = LocalDate.of(2026, 6, 30);

  @Autowired private ReceivablesFixtures fx;
  @Autowired private BankStatementService statements;
  @Autowired private BankMatchingService matching;
  @Autowired private BankReconciliationService reconciliation;
  @Autowired private AsUser as;

  private Receipt receipt(String amount, int day) {
    return fx.approved(
        fx.request(
                PayerType.POLICYHOLDER,
                "C-000201",
                LocalDate.of(2026, 6, day),
                ReceiptMode.BANK_TRANSFER,
                amount,
                CASH)
            .with(AllocationMethod.NONE, List.of()));
  }

  private BankStatement importCsv(String ref, String csv) {
    return as.run(
        "accountant",
        () ->
            statements.importStatement(
                new StatementImportRequest(fx.company(), CASH, ref, ref + ".csv", csv, null)));
  }

  @Test
  void statementIsMatchedReconciledAndFinalized() {
    Receipt r1 = receipt("5000.00", 2);
    Receipt r2 = receipt("7500.00", 3);
    receipt("2000.00", 5);

    BankStatement june =
        importCsv(
            "JUNE-A",
            "Date,Description,Reference,Debit,Credit,Balance\n"
                + "03/06/2026,Transfer in,"
                + r1.getReceiptNo()
                + ",,\"5,000.00\",5000.00\n"
                + "2026-06-04,Transfer in,,,7500.00,12500.00\n"
                + "06-06-2026,\"Service charge, June\",SC1,150.00,,12350.00\n");
    assertThat(june.getLineCount()).isEqualTo(3);
    assertThat(june.getOpeningBalance()).isZero();
    assertThat(june.getClosingBalance()).isEqualByComparingTo("12350.00");
    assertThat(statements.list(fx.company(), CASH))
        .extracting(BankStatement::getId)
        .contains(june.getId());
    assertThatThrownBy(() -> importCsv("JUNE-A", "date,debit,credit\n2026-06-01,1.00,\n"))
        .isInstanceOf(DuplicateResourceException.class);

    List<BankMatch> auto =
        as.run(
            "accountant",
            () -> matching.autoMatch(new AutoMatchRequest(fx.company(), CASH, JUNE_END, null)));
    assertThat(auto).hasSize(2);
    assertThat(statements.lines(june.getId()))
        .filteredOn(l -> l.getMatchId() != null)
        .extracting(BankStatementLine::getLineNo)
        .containsExactlyInAnyOrder(1, 2);

    var brs = reconciliation.statement(fx.company(), CASH, JUNE_END);
    assertThat(brs.bookDebits())
        .extracting(BookEntry::debit)
        .containsExactly(new BigDecimal("2000.00"));
    assertThat(brs.bankDebits()).hasSize(1);
    assertThat(brs.figures().bookBalance()).isEqualByComparingTo("14500.00");
    assertThat(brs.figures().statementBalance()).isEqualByComparingTo("12350.00");
    assertThat(brs.figures().computedBankBalance()).isEqualByComparingTo("12350.00");
    assertThat(brs.figures().difference()).isZero();

    BankStatement july =
        importCsv(
            "JUNE-B",
            "date,description,reference,debit,credit\n2026-06-20,Cash deposit,,,2000.00\n");
    assertThat(july.getOpeningBalance()).isZero();
    var workbench = matching.workbench(fx.company(), CASH, JUNE_END);
    assertThat(workbench.bookEntries()).hasSize(1);
    Long lineId =
        workbench.bankLines().stream()
            .filter(l -> l.getStatementId().equals(july.getId()))
            .findFirst()
            .orElseThrow()
            .getId();
    Long entryId = workbench.bookEntries().get(0).id();
    assertThatThrownBy(
            () ->
                matching.manualMatch(
                    new ManualMatchRequest(
                        fx.company(),
                        CASH,
                        List.of(entryId),
                        workbench.bankLines().stream().map(BankStatementLine::getId).toList())))
        .hasMessageContaining("differs");
    BankMatch manual =
        as.run(
            "accountant",
            () ->
                matching.manualMatch(
                    new ManualMatchRequest(fx.company(), CASH, List.of(entryId), List.of(lineId))));
    assertThat(manual.getMatchDate()).isEqualTo(LocalDate.of(2026, 6, 20));
    as.run("accountant", () -> matching.unmatch(manual.getId()));
    assertThatThrownBy(
            () ->
                matching.manualMatch(
                    new ManualMatchRequest(fx.company(), CASH, List.of(-1L), List.of(lineId))))
        .isInstanceOf(BusinessRuleException.class);
    as.run(
        "accountant",
        () ->
            matching.manualMatch(
                new ManualMatchRequest(fx.company(), CASH, List.of(entryId), List.of(lineId))));
    assertThat(matching.recent(fx.company(), CASH)).hasSize(3);

    // The balance of statement B is its own running balance (opening 0), so as of June 30 the
    // statement balance is 2000 and the BRS shows the unexplained difference.
    var rec =
        as.run(
            "fmanager",
            () -> reconciliation.save(new ReconciliationRequest(fx.company(), CASH, JUNE_END)));
    assertThat(rec.getStatus()).isEqualTo(ReconciliationStatus.IN_PROGRESS);
    assertThat(rec.getDifference()).isNotZero();
    assertThatThrownBy(
            () -> as.run("fmanager", () -> reconciliation.finalizeReconciliation(rec.getId())))
        .hasMessageContaining("difference");

    var june5 =
        as.run(
            "fmanager",
            () ->
                reconciliation.save(
                    new ReconciliationRequest(fx.company(), CASH, LocalDate.of(2026, 6, 10))));
    var finalized = as.run("fmanager", () -> reconciliation.finalizeReconciliation(june5.getId()));
    assertThat(finalized.getStatus()).isEqualTo(ReconciliationStatus.FINALIZED);
    assertThat(reconciliation.list(fx.company()))
        .extracting(r -> r.getId())
        .contains(finalized.getId());
    assertThatThrownBy(() -> as.run("accountant", () -> matching.unmatch(auto.get(0).getId())))
        .hasMessageContaining("finalized");
    assertThatThrownBy(
            () ->
                as.run(
                    "fmanager",
                    () ->
                        reconciliation.save(
                            new ReconciliationRequest(
                                fx.company(), CASH, LocalDate.of(2026, 6, 10)))))
        .isInstanceOf(BusinessRuleException.class);

    // Offsetting items of one side only (a wrong charge and its reversal) match off at zero.
    BankStatement julyC =
        importCsv(
            "JULY-C",
            "date,description,reference,debit,credit\n"
                + "2026-07-02,Charge in error,E1,300.00,\n"
                + "2026-07-03,Reversal of charge,E1,,300.00\n");
    List<Long> pair =
        statements.lines(julyC.getId()).stream().map(BankStatementLine::getId).toList();
    assertThatThrownBy(
            () ->
                matching.manualMatch(
                    new ManualMatchRequest(fx.company(), CASH, List.of(), pair.subList(0, 1))))
        .isInstanceOf(BusinessRuleException.class);
    BankMatch offset =
        as.run(
            "accountant",
            () ->
                matching.manualMatch(new ManualMatchRequest(fx.company(), CASH, List.of(), pair)));
    assertThat(offset.getAmount()).isZero();
    assertThat(statements.lines(julyC.getId()))
        .allMatch(l -> offset.getId().equals(l.getMatchId()));
  }

  @Test
  void invalidStatementsAreRejected() {
    assertThatThrownBy(() -> importCsv("BAD-1", "date,debit,credit\n"))
        .hasMessageContaining("no lines");
    assertThatThrownBy(() -> importCsv("BAD-2", "when,debit,credit\n2026-06-01,1,\n"))
        .hasMessageContaining("Missing column");
    assertThatThrownBy(() -> importCsv("BAD-3", "date,debit,credit\n2026/06/01,1,\n"))
        .hasMessageContaining("invalid date");
    assertThatThrownBy(() -> importCsv("BAD-4", "date,debit,credit\n2026-06-01,abc,\n"))
        .hasMessageContaining("invalid amount");
    assertThatThrownBy(() -> importCsv("BAD-5", "date,debit,credit\n2026-06-01,1.00,2.00\n"))
        .hasMessageContaining("positive debit or a positive credit");
    assertThatThrownBy(
            () ->
                importCsv(
                    "BAD-6",
                    "date,debit,credit,balance\n2026-06-01,,5.00,5.00\n2026-06-02,,1.00,9.00\n"))
        .hasMessageContaining("does not agree");
  }
}
