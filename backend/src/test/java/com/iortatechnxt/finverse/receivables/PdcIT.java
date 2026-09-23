package com.iortatechnxt.finverse.receivables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.receivables.api.dto.DateRequest;
import com.iortatechnxt.finverse.receivables.api.dto.PdcReplaceRequest;
import com.iortatechnxt.finverse.receivables.api.dto.PdcRequest;
import com.iortatechnxt.finverse.receivables.api.dto.ReversalRequest;
import com.iortatechnxt.finverse.receivables.domain.AllocationMethod;
import com.iortatechnxt.finverse.receivables.domain.PdcStatus;
import com.iortatechnxt.finverse.receivables.domain.PostDatedCheque;
import com.iortatechnxt.finverse.receivables.domain.Receipt;
import com.iortatechnxt.finverse.receivables.domain.ReceiptMode;
import com.iortatechnxt.finverse.receivables.domain.ReceiptStatus;
import com.iortatechnxt.finverse.receivables.service.PdcQueries;
import com.iortatechnxt.finverse.receivables.service.PdcService;
import com.iortatechnxt.finverse.receivables.service.ReceiptService;
import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class PdcIT {

  private static final LocalDate RECEIVED = LocalDate.of(2026, 3, 2);
  private static final LocalDate DUE = LocalDate.of(2026, 4, 1);

  @Autowired private ReceivablesFixtures fx;
  @Autowired private PdcService pdcs;
  @Autowired private PdcQueries queries;
  @Autowired private ReceiptService receipts;
  @Autowired private AsUser as;

  private PostDatedCheque register(String party, String amount, Long debitItemId) {
    return as.run(
        "accountant",
        () ->
            pdcs.register(
                new PdcRequest(
                    fx.company(),
                    fx.branch(),
                    RECEIVED,
                    party,
                    "FND",
                    "PDC" + System.nanoTime() % 1_000_000,
                    DUE,
                    "Metrobank",
                    "PHP",
                    new BigDecimal(amount),
                    "1111",
                    debitItemId,
                    null)));
  }

  @Test
  void pdcIsBankedAsReceiptAndCleared() {
    OpenItem note = fx.debitNote("C-000204", RECEIVED.minusDays(5), "12000.00", "PHP");
    PostDatedCheque pdc = register("C-000204", "12000.00", note.getId());
    assertThat(pdc.getStatus()).isEqualTo(PdcStatus.ON_HAND);
    assertThat(pdc.getPdcNo()).startsWith("PDC-HO-2026-");
    assertThat(queries.heldAsOf(fx.company(), RECEIVED))
        .extracting(p -> p.pdc().getId())
        .contains(pdc.getId());

    assertThatThrownBy(
            () ->
                as.run(
                    "accountant",
                    () -> pdcs.deposit(pdc.getId(), new DateRequest(RECEIVED.plusDays(1), null))))
        .hasMessageContaining("dated");
    assertThat(as.run("accountant", () -> pdcs.markDue(fx.company(), DUE)))
        .extracting(PostDatedCheque::getId)
        .contains(pdc.getId());
    Receipt receipt =
        as.run("accountant", () -> pdcs.deposit(pdc.getId(), new DateRequest(DUE, "banked")));
    assertThat(receipt.getMode()).isEqualTo(ReceiptMode.PDC);
    assertThat(receipt.getAllocationMethod()).isEqualTo(AllocationMethod.MANUAL);
    assertThatThrownBy(
            () -> as.run("accountant", () -> pdcs.clear(pdc.getId(), new DateRequest(DUE, null))))
        .hasMessageContaining("PENDING_APPROVAL");

    fx.approve(receipt);
    assertThat(fx.outstanding(note)).isZero();
    PostDatedCheque cleared =
        as.run("accountant", () -> pdcs.clear(pdc.getId(), new DateRequest(DUE.plusDays(2), "ok")));
    assertThat(cleared.getStatus()).isEqualTo(PdcStatus.CLEARED);
    assertThat(pdcs.history(pdc.getId()))
        .extracting(e -> e.getToStatus())
        .containsExactly(PdcStatus.ON_HAND, PdcStatus.DUE, PdcStatus.DEPOSITED, PdcStatus.CLEARED);
    assertThat(queries.asOf(fx.company(), RECEIVED.plusDays(1)))
        .filteredOn(p -> p.pdc().getId().equals(pdc.getId()))
        .extracting(PdcQueries.PdcAsOf::status)
        .containsExactly(PdcStatus.ON_HAND);
    assertThat(queries.receivedBetween(fx.company(), RECEIVED, RECEIVED))
        .extracting(p -> p.pdc().getId())
        .contains(pdc.getId());
  }

  @Test
  void bouncedPdcReversesItsReceipt() {
    OpenItem note = fx.debitNote("C-000204", RECEIVED.minusDays(3), "3000.00", "PHP");
    PostDatedCheque pdc = register("C-000204", "3000.00", null);
    Receipt receipt =
        as.run("accountant", () -> pdcs.deposit(pdc.getId(), new DateRequest(DUE, null)));
    assertThat(receipt.getAllocationMethod()).isEqualTo(AllocationMethod.FIFO);
    fx.approve(receipt);
    as.run("checker", () -> pdcs.bounce(pdc.getId(), new ReversalRequest(DUE.plusDays(3), "NSF")));
    assertThat(pdcs.get(pdc.getId()).getStatus()).isEqualTo(PdcStatus.BOUNCED);
    assertThat(receipts.get(receipt.getId()).getStatus()).isEqualTo(ReceiptStatus.BOUNCED);
    assertThat(fx.outstanding(note)).isPositive();
    assertThatThrownBy(
            () ->
                as.run(
                    "checker",
                    () -> pdcs.bounce(pdc.getId(), new ReversalRequest(DUE.plusDays(4), "again"))))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void rejectedReceiptPutsPdcBackToDue() {
    PostDatedCheque pdc = register("C-000204", "900.00", null);
    Receipt receipt =
        as.run("accountant", () -> pdcs.deposit(pdc.getId(), new DateRequest(DUE, null)));
    as.run(
        "checker",
        () -> receipts.reject(receipt.getId(), new ReversalRequest(DUE, "Wrong bank account")));
    PostDatedCheque back = pdcs.get(pdc.getId());
    assertThat(back.getStatus()).isEqualTo(PdcStatus.DUE);
    assertThat(back.getReceiptId()).isNull();
  }

  @Test
  void pdcCanBeReturnedOrReplaced() {
    PostDatedCheque returned = register("A-0001", "500.00", null);
    as.run(
        "accountant",
        () -> pdcs.returnCheque(returned.getId(), new ReversalRequest(RECEIVED, "Paid in cash")));
    assertThat(pdcs.get(returned.getId()).getStatus()).isEqualTo(PdcStatus.RETURNED);
    assertThatThrownBy(
            () ->
                as.run(
                    "accountant", () -> pdcs.deposit(returned.getId(), new DateRequest(DUE, null))))
        .isInstanceOf(BusinessRuleException.class);

    PostDatedCheque old = register("A-0001", "800.00", null);
    PostDatedCheque replacement =
        as.run(
            "accountant",
            () ->
                pdcs.replace(
                    old.getId(),
                    new PdcReplaceRequest(
                        RECEIVED.plusDays(3),
                        "NEW-123",
                        DUE.plusDays(15),
                        "BPI",
                        new BigDecimal("800.00"),
                        "Stale cheque replaced")));
    assertThat(pdcs.get(old.getId()).getStatus()).isEqualTo(PdcStatus.REPLACED);
    assertThat(pdcs.get(old.getId()).getReplacedById()).isEqualTo(replacement.getId());
    assertThat(replacement.getStatus()).isEqualTo(PdcStatus.ON_HAND);
    assertThat(pdcs.list(fx.company(), PdcStatus.REPLACED))
        .extracting(PostDatedCheque::getId)
        .contains(old.getId());
    assertThat(pdcs.list(fx.company(), null)).isNotEmpty();
  }

  @Test
  void invalidPdcsAreRejected() {
    assertThatThrownBy(
            () ->
                as.run(
                    "accountant",
                    () ->
                        pdcs.register(
                            new PdcRequest(
                                fx.company(),
                                fx.branch(),
                                DUE,
                                "A-0001",
                                null,
                                "X1",
                                RECEIVED,
                                "BPI",
                                "PHP",
                                BigDecimal.TEN,
                                "1111",
                                null,
                                null))))
        .hasMessageContaining("cheque date");
    OpenItem other = fx.debitNote("C-000102", RECEIVED, "100.00", "PHP");
    assertThatThrownBy(() -> register("A-0001", "100.00", other.getId()))
        .hasMessageContaining("debit item of the payer");
    assertThatThrownBy(() -> register("S-0001", "100.00", null))
        .isInstanceOf(BusinessRuleException.class);
  }
}
