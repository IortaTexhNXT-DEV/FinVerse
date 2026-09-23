package com.iortatechnxt.finverse.receivables;

import static com.iortatechnxt.finverse.receivables.ReceivablesFixtures.alloc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.receivables.api.dto.AllocationRequest;
import com.iortatechnxt.finverse.receivables.api.dto.ApplyRequest;
import com.iortatechnxt.finverse.receivables.api.dto.DateRequest;
import com.iortatechnxt.finverse.receivables.api.dto.DepositSlipRequest;
import com.iortatechnxt.finverse.receivables.api.dto.ReversalRequest;
import com.iortatechnxt.finverse.receivables.domain.AllocationMethod;
import com.iortatechnxt.finverse.receivables.domain.DepositSlipStatus;
import com.iortatechnxt.finverse.receivables.domain.DepositStatus;
import com.iortatechnxt.finverse.receivables.domain.PayerType;
import com.iortatechnxt.finverse.receivables.domain.Receipt;
import com.iortatechnxt.finverse.receivables.domain.ReceiptMode;
import com.iortatechnxt.finverse.receivables.domain.ReceiptSearch;
import com.iortatechnxt.finverse.receivables.domain.ReceiptStatus;
import com.iortatechnxt.finverse.receivables.service.DepositService;
import com.iortatechnxt.finverse.receivables.service.ReceiptPostingService;
import com.iortatechnxt.finverse.receivables.service.ReceiptService;
import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import com.iortatechnxt.finverse.subledger.domain.OpenItemStatus;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

@IntegrationTest
class ReceiptFlowIT {

  private static final LocalDate MAY = LocalDate.of(2026, 5, 10);
  private static final String BANK = "1112";

  @Autowired private ReceivablesFixtures fx;
  @Autowired private ReceiptService receipts;
  @Autowired private ReceiptPostingService posting;
  @Autowired private DepositService deposits;
  @Autowired private OpenItemService openItems;
  @Autowired private AsUser as;

  @Test
  void approvalPostsJournalsAndMatchesDebitNotes() {
    OpenItem first = fx.debitNote("C-000201", MAY, "10000.00", "PHP");
    OpenItem second = fx.debitNote("C-000201", MAY.plusDays(1), "5000.00", "PHP");
    BigDecimal bankBefore = fx.balance(BANK);
    BigDecimal depositsBefore = fx.balance("2205");
    List<AllocationRequest> allocations =
        List.of(
            new AllocationRequest(first.getId(), new BigDecimal("10000.00")),
            new AllocationRequest(second.getId(), new BigDecimal("2000.00")));
    Receipt created =
        fx.create(
            fx.request(
                    PayerType.POLICYHOLDER,
                    "C-000201",
                    MAY.plusDays(5),
                    ReceiptMode.BANK_TRANSFER,
                    "15000.00",
                    BANK)
                .with(AllocationMethod.MANUAL, allocations));
    assertThat(created.getStatus()).isEqualTo(ReceiptStatus.PENDING_APPROVAL);
    assertThat(created.getReceiptNo()).startsWith("OR-HO-2026-");
    assertThat(created.getDepositStatus()).isEqualTo(DepositStatus.NOT_REQUIRED);

    Receipt approved = fx.approve(created);

    assertThat(approved.getStatus()).isEqualTo(ReceiptStatus.APPROVED);
    assertThat(approved.getAppliedAmount()).isEqualByComparingTo("12000.00");
    assertThat(approved.unapplied()).isEqualByComparingTo("3000.00");
    assertThat(approved.getJournalBatchNo()).isNotBlank();
    assertThat(openItems.get(first.getId()).getStatus()).isEqualTo(OpenItemStatus.SETTLED);
    assertThat(fx.outstanding(second)).isEqualByComparingTo("3000.00");
    assertThat(openItems.get(approved.getCreditItemId()).outstanding())
        .isEqualByComparingTo("3000.00");
    assertThat(fx.balance(BANK).subtract(bankBefore)).isEqualByComparingTo("15000.00");
    assertThat(fx.balance("2205").subtract(depositsBefore)).isEqualByComparingTo("-3000.00");
    assertThat(receipts.get(approved.getId()).getAllocations())
        .allSatisfy(a -> assertThat(a.getMatchId()).isNotNull());
    assertThat(
            receipts
                .search(
                    new ReceiptSearch(
                        fx.company(),
                        ReceiptStatus.APPROVED,
                        "C-000201",
                        ReceiptMode.BANK_TRANSFER,
                        MAY,
                        MAY.plusDays(10),
                        approved.getReceiptNo().substring(3)),
                    PageRequest.of(0, 10))
                .getContent())
        .extracting(Receipt::getId)
        .contains(approved.getId());
  }

  @Test
  void makerCannotApproveOwnReceipt() {
    Receipt r =
        fx.create(
            fx.request(PayerType.POLICYHOLDER, "C-000201", MAY, ReceiptMode.CASH, "100.00", BANK)
                .with(AllocationMethod.NONE, List.of()));
    assertThatThrownBy(() -> as.run("accountant", () -> posting.approve(r.getId())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("cannot be approved");
    Receipt rejected =
        as.run("checker", () -> receipts.reject(r.getId(), new ReversalRequest(MAY, "Duplicate")));
    assertThat(rejected.getStatus()).isEqualTo(ReceiptStatus.REJECTED);
    assertThatThrownBy(() -> fx.approve(r)).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void onAccountMoneyIsAppliedLaterFifo() {
    String agent = fx.newAgent();
    OpenItem older = fx.debitNote(agent, MAY, "4000.00", "PHP");
    Receipt r =
        fx.approved(
            fx.request(
                    PayerType.INTERMEDIARY,
                    agent,
                    MAY.plusDays(2),
                    ReceiptMode.CHEQUE,
                    "9000.00",
                    BANK)
                .with(AllocationMethod.FIFO, List.of()));
    assertThat(r.getAppliedAmount()).isEqualByComparingTo("4000.00");
    assertThat(fx.outstanding(older)).isZero();

    OpenItem later = fx.debitNote(agent, MAY.plusDays(20), "3500.00", "PHP");
    BigDecimal depositsBefore = fx.balance("2205");
    Receipt applied =
        as.run(
            "accountant",
            () ->
                posting.applyUnapplied(
                    r.getId(),
                    new ApplyRequest(MAY.plusDays(21), AllocationMethod.FIFO, List.of())));
    assertThat(applied.getAppliedAmount()).isEqualByComparingTo("7500.00");
    assertThat(applied.unapplied()).isEqualByComparingTo("1500.00");
    assertThat(fx.outstanding(later)).isZero();
    assertThat(fx.balance("2205").subtract(depositsBefore)).isEqualByComparingTo("3500.00");
    assertThatThrownBy(
            () ->
                as.run(
                    "accountant",
                    () ->
                        posting.applyUnapplied(
                            r.getId(),
                            new ApplyRequest(MAY.plusDays(21), AllocationMethod.FIFO, List.of()))))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void cancellationReversesJournalsAndReopensDebitNotes() {
    OpenItem note = fx.debitNote("C-000201", MAY, "8000.00", "PHP");
    BigDecimal bankBefore = fx.balance(BANK);
    Receipt r =
        fx.approved(
            fx.request(
                    PayerType.POLICYHOLDER,
                    "C-000201",
                    MAY.plusDays(3),
                    ReceiptMode.BANK_TRANSFER,
                    "9000.00",
                    BANK)
                .with(AllocationMethod.MANUAL, alloc(note, "8000.00")));
    assertThat(fx.outstanding(note)).isZero();

    Receipt cancelled =
        as.run(
            "checker",
            () -> posting.cancel(r.getId(), new ReversalRequest(MAY.plusDays(4), "Wrong payer")));

    assertThat(cancelled.getStatus()).isEqualTo(ReceiptStatus.CANCELLED);
    assertThat(fx.outstanding(note)).isEqualByComparingTo("8000.00");
    assertThat(openItems.get(r.getCreditItemId()).outstanding()).isZero();
    assertThat(fx.balance(BANK)).isEqualByComparingTo(bankBefore);
    assertThat(receipts.get(r.getId()).getAllocations())
        .allSatisfy(a -> assertThat(a.getMatchId()).isNull());
    assertThatThrownBy(
            () ->
                as.run(
                    "checker", () -> posting.cancel(r.getId(), new ReversalRequest(MAY, "again"))))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void depositedChequeBouncesAndReopensDebitNote() {
    OpenItem note = fx.debitNote("B-0002", MAY, "6000.00", "PHP");
    Receipt r =
        fx.approved(
            fx.request(
                    PayerType.INTERMEDIARY,
                    "B-0002",
                    MAY.plusDays(1),
                    ReceiptMode.CHEQUE,
                    "6000.00",
                    BANK)
                .with(AllocationMethod.MANUAL, alloc(note, "6000.00")));
    assertThat(deposits.undeposited(fx.company(), BANK))
        .extracting(Receipt::getId)
        .contains(r.getId());

    var slip =
        as.run(
            "accountant",
            () ->
                deposits.create(
                    new DepositSlipRequest(
                        fx.company(), fx.branch(), BANK, MAY.plusDays(2), List.of(r.getId()))));
    assertThat(slip.getTotalAmount()).isEqualByComparingTo("6000.00");
    assertThatThrownBy(
            () ->
                as.run(
                    "checker",
                    () -> posting.bounce(r.getId(), new ReversalRequest(MAY.plusDays(3), "x"))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("deposit slip");
    var deposited =
        as.run(
            "accountant",
            () -> deposits.confirm(slip.getId(), new DateRequest(MAY.plusDays(2), null)));
    assertThat(deposited.getStatus()).isEqualTo(DepositSlipStatus.DEPOSITED);
    assertThat(receipts.get(r.getId()).getDepositStatus()).isEqualTo(DepositStatus.DEPOSITED);

    Receipt bounced =
        as.run(
            "checker",
            () ->
                posting.bounce(
                    r.getId(), new ReversalRequest(MAY.plusDays(6), "Insufficient funds")));

    assertThat(bounced.getStatus()).isEqualTo(ReceiptStatus.BOUNCED);
    assertThat(fx.outstanding(note)).isEqualByComparingTo("6000.00");
    assertThat(deposits.receiptsOf(slip.getId())).hasSize(1);
  }

  @Test
  void preparedSlipCanBeCancelled() {
    Receipt r =
        fx.approved(
            fx.request(PayerType.POLICYHOLDER, "C-000201", MAY, ReceiptMode.CASH, "700.00", BANK)
                .with(AllocationMethod.NONE, List.of()));
    var slip =
        as.run(
            "accountant",
            () ->
                deposits.create(
                    new DepositSlipRequest(
                        fx.company(), fx.branch(), BANK, MAY, List.of(r.getId()))));
    var cancelled = as.run("accountant", () -> deposits.cancel(slip.getId()));
    assertThat(cancelled.getStatus()).isEqualTo(DepositSlipStatus.CANCELLED);
    assertThat(receipts.get(r.getId()).getDepositStatus()).isEqualTo(DepositStatus.UNDEPOSITED);
    assertThat(deposits.list(fx.company())).extracting(s -> s.getId()).contains(slip.getId());
    assertThatThrownBy(() -> as.run("accountant", () -> deposits.cancel(slip.getId())))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void reinsurerAndOtherIncomeReceipts() {
    BigDecimal incomeBefore = fx.balance("4700");
    Receipt other =
        fx.approved(
            fx.request(PayerType.OTHER, null, MAY, ReceiptMode.CASH, "1234.50", BANK)
                .with(AllocationMethod.MANUAL, List.of()));
    assertThat(other.getCreditItemId()).isNull();
    assertThat(other.getAllocationMethod()).isEqualTo(AllocationMethod.NONE);
    assertThat(fx.balance("4700").subtract(incomeBefore)).isEqualByComparingTo("-1234.50");

    Receipt ri =
        fx.approved(
            fx.request(
                    PayerType.REINSURER, "R-0002", MAY, ReceiptMode.BANK_TRANSFER, "25000.00", BANK)
                .with(AllocationMethod.FIFO, List.of()));
    assertThat(ri.getStatus()).isEqualTo(ReceiptStatus.APPROVED);
    assertThat(openItems.get(ri.getCreditItemId()).outstanding()).isEqualByComparingTo("25000.00");
    as.run(
        "checker", () -> posting.cancel(ri.getId(), new ReversalRequest(MAY.plusDays(1), "error")));
    as.run(
        "checker",
        () -> posting.cancel(other.getId(), new ReversalRequest(MAY.plusDays(1), "error")));
    assertThat(fx.balance("4700")).isEqualByComparingTo(incomeBefore);
  }

  @Test
  void invalidReceiptsAreRejected() {
    OpenItem note = fx.debitNote("C-000201", MAY, "100.00", "PHP");
    assertThatThrownBy(
            () ->
                fx.create(
                    fx.request(
                            PayerType.POLICYHOLDER, "C-000201", MAY, ReceiptMode.PDC, "10.00", BANK)
                        .with(AllocationMethod.NONE, List.of())))
        .hasMessageContaining("PDC register");
    assertThatThrownBy(
            () ->
                fx.create(
                    fx.request(
                            PayerType.POLICYHOLDER,
                            "C-000201",
                            MAY,
                            ReceiptMode.CASH,
                            "10.00",
                            BANK)
                        .with(AllocationMethod.MANUAL, alloc(note, "50.00"))))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(
            () ->
                fx.create(
                    fx.request(
                            PayerType.POLICYHOLDER,
                            "C-000201",
                            MAY,
                            ReceiptMode.CASH,
                            "500.00",
                            BANK)
                        .with(AllocationMethod.MANUAL, alloc(note, "150.00"))))
        .hasMessageContaining("exceeds the outstanding");
    assertThatThrownBy(
            () ->
                fx.create(
                    fx.request(
                            PayerType.POLICYHOLDER,
                            "C-000102",
                            MAY,
                            ReceiptMode.CASH,
                            "500.00",
                            BANK)
                        .with(AllocationMethod.MANUAL, alloc(note, "50.00"))))
        .hasMessageContaining("debit item of the payer");
    assertThatThrownBy(
            () ->
                fx.create(
                    fx.request(
                            PayerType.POLICYHOLDER,
                            "C-000201",
                            MAY,
                            ReceiptMode.CASH,
                            "500.00",
                            BANK)
                        .with(AllocationMethod.MANUAL, List.of())))
        .hasMessageContaining("at least one");
    assertThatThrownBy(
            () ->
                fx.create(
                    fx.request(
                            PayerType.POLICYHOLDER,
                            "C-000201",
                            MAY,
                            ReceiptMode.CASH,
                            "500.00",
                            "4700")
                        .with(AllocationMethod.NONE, List.of())))
        .hasMessageContaining("not an active bank");
    assertThatThrownBy(
            () ->
                fx.create(
                    fx.request(
                            PayerType.REINSURER, "C-000201", MAY, ReceiptMode.CASH, "500.00", BANK)
                        .with(AllocationMethod.NONE, List.of())))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(receipts.openDebitItems(fx.company(), "C-000201", "PHP"))
        .extracting(OpenItem::getId)
        .contains(note.getId());
  }
}
