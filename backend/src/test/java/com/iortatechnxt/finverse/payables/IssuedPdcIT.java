package com.iortatechnxt.finverse.payables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.payables.domain.IssuedPdc;
import com.iortatechnxt.finverse.payables.domain.IssuedPdcEvent;
import com.iortatechnxt.finverse.payables.domain.IssuedPdcStatus;
import com.iortatechnxt.finverse.payables.domain.PaymentMode;
import com.iortatechnxt.finverse.payables.domain.PaymentVoucher;
import com.iortatechnxt.finverse.payables.domain.SupplierInvoice;
import com.iortatechnxt.finverse.payables.domain.VoucherStatus;
import com.iortatechnxt.finverse.payables.service.IssuedPdcService;
import com.iortatechnxt.finverse.payables.service.PaymentVoucherService;
import com.iortatechnxt.finverse.subledger.domain.OpenItemStatus;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class IssuedPdcIT {

  private static final LocalDate ISSUE = LocalDate.of(2026, 9, 2);
  private static final LocalDate CHEQUE_DATE = LocalDate.of(2026, 9, 12);

  @Autowired private IssuedPdcService pdcs;
  @Autowired private PaymentVoucherService vouchers;
  @Autowired private OpenItemService openItems;
  @Autowired private PayablesFixtures fx;
  @Autowired private AsUser as;

  private IssuedPdc issue(String party, String net) {
    SupplierInvoice inv = fx.approvedInvoice(party, net, PayablesFixtures.DATE);
    PaymentVoucher v =
        fx.approvedPayment(
            fx.paymentCommand(
                party,
                PaymentMode.PDC,
                "BDO-CA",
                ISSUE,
                CHEQUE_DATE,
                Map.of(inv.getOpenItemId(), inv.getPayableAmount())));
    assertThat(openItems.get(inv.getOpenItemId()).getStatus()).isEqualTo(OpenItemStatus.SETTLED);
    assertThat(fx.posted(v.getJournalBatchNo(), "2511"))
        .isEqualByComparingTo(v.getAmount().negate());
    assertThat(fx.posted(v.getJournalBatchNo(), "1111")).isEqualByComparingTo(BigDecimal.ZERO);
    return pdcs
        .search(fx.companyId(), EnumSet.allOf(IssuedPdcStatus.class), CHEQUE_DATE, CHEQUE_DATE)
        .stream()
        .filter(p -> p.getVoucherId().equals(v.getId()))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void issuedChequeBecomesDueIsPresentedAndCleared() {
    IssuedPdc pdc = issue("S-0003", "5000.00");
    assertThat(pdc.getStatus()).isEqualTo(IssuedPdcStatus.ISSUED);
    assertThatThrownBy(() -> as.run("checker", () -> pdcs.present(pdc.getId(), ISSUE)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("dated");

    assertThat(as.run("accountant", () -> pdcs.refreshDue(CHEQUE_DATE))).isPositive();
    assertThat(pdcs.get(pdc.getId()).getStatus()).isEqualTo(IssuedPdcStatus.DUE);

    IssuedPdc presented =
        as.run("checker", () -> pdcs.present(pdc.getId(), CHEQUE_DATE.plusDays(1)));
    assertThat(presented.getStatus()).isEqualTo(IssuedPdcStatus.PRESENTED);
    assertThat(fx.posted(presented.getPresentationBatchNo(), "2511"))
        .isEqualByComparingTo(pdc.getAmount());
    assertThat(fx.posted(presented.getPresentationBatchNo(), "1111"))
        .isEqualByComparingTo(pdc.getAmount().negate());

    IssuedPdc cleared =
        as.run("accountant", () -> pdcs.clear(pdc.getId(), CHEQUE_DATE.plusDays(2)));
    assertThat(cleared.getStatus()).isEqualTo(IssuedPdcStatus.CLEARED);
    assertThat(pdcs.history(pdc.getId()))
        .extracting(IssuedPdcEvent::getToStatus)
        .containsExactly(
            IssuedPdcStatus.ISSUED,
            IssuedPdcStatus.DUE,
            IssuedPdcStatus.PRESENTED,
            IssuedPdcStatus.CLEARED);
    assertThatThrownBy(() -> as.run("checker", () -> pdcs.cancel(pdc.getId(), CHEQUE_DATE, "late")))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void cancellingAnOutstandingChequeReversesThePaymentAndReinstatesThePayable() {
    IssuedPdc pdc = issue("S-0003", "2000.00");
    IssuedPdc cancelled =
        as.run("checker", () -> pdcs.cancel(pdc.getId(), ISSUE.plusDays(3), "Stop payment"));
    assertThat(cancelled.getStatus()).isEqualTo(IssuedPdcStatus.CANCELLED);
    assertThat(fx.posted(cancelled.getCancelBatchNo(), "2511"))
        .isEqualByComparingTo(pdc.getAmount());
    PaymentVoucher voucher = vouchers.get(pdc.getVoucherId());
    assertThat(voucher.getStatus()).isEqualTo(VoucherStatus.VOIDED);
    assertThat(vouchers.payableItems(fx.companyId(), "S-0003", null))
        .anySatisfy(p -> assertThat(p.available()).isEqualByComparingTo(pdc.getAmount()));
  }

  @Test
  void replacingAChequeIssuesANewLeafForTheSamePayment() {
    IssuedPdc pdc = issue("S-0001", "700.00");
    IssuedPdc replacement =
        as.run(
            "checker",
            () -> pdcs.replace(pdc.getId(), CHEQUE_DATE.plusDays(5), ISSUE.plusDays(1), "Damaged"));
    assertThat(replacement.getStatus()).isEqualTo(IssuedPdcStatus.ISSUED);
    assertThat(replacement.getChequeNo()).isNotEqualTo(pdc.getChequeNo());
    assertThat(replacement.getReplacesId()).isEqualTo(pdc.getId());
    assertThat(pdcs.get(pdc.getId()).getStatus()).isEqualTo(IssuedPdcStatus.REPLACED);
    assertThat(vouchers.get(pdc.getVoucherId()).getChequeNo()).isEqualTo(replacement.getChequeNo());
  }
}
