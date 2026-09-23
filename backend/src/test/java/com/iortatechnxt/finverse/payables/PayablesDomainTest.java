package com.iortatechnxt.finverse.payables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.party.domain.PartyType;
import com.iortatechnxt.finverse.payables.domain.ChequeBook;
import com.iortatechnxt.finverse.payables.domain.ChequeBookStatus;
import com.iortatechnxt.finverse.payables.domain.InvoiceCalculator;
import com.iortatechnxt.finverse.payables.domain.IssuedPdcStatus;
import com.iortatechnxt.finverse.payables.domain.PaymentCategory;
import com.iortatechnxt.finverse.payables.domain.PaymentMode;
import com.iortatechnxt.finverse.payables.domain.PettyCashFund;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PayablesDomainTest {

  @Test
  void invoiceTaxesAreComputedPerLine() {
    BigDecimal net = new BigDecimal("1234.56");
    assertThat(InvoiceCalculator.vat(net, true)).isEqualByComparingTo("148.15");
    assertThat(InvoiceCalculator.vat(net, false)).isEqualByComparingTo("0.00");
    assertThat(InvoiceCalculator.withholding(net, new BigDecimal("2")))
        .isEqualByComparingTo("24.69");
    assertThat(InvoiceCalculator.withholding(net, null)).isEqualByComparingTo("0");
    assertThat(InvoiceCalculator.withholding(net, BigDecimal.ZERO)).isEqualByComparingTo("0");
    assertThat(InvoiceCalculator.payable(net, new BigDecimal("148.15"), new BigDecimal("24.69")))
        .isEqualByComparingTo("1358.02");
  }

  @Test
  void chequeBookHandsOutPaddedLeavesUntilExhausted() {
    ChequeBook book = new ChequeBook(1L, 98, 100, LocalDate.of(2026, 1, 1));
    assertThat(book.remaining()).isEqualTo(3);
    assertThat(book.allocate()).isEqualTo("098");
    assertThat(book.allocate()).isEqualTo("099");
    assertThat(book.allocate()).isEqualTo("100");
    assertThat(book.getStatus()).isEqualTo(ChequeBookStatus.EXHAUSTED);
    assertThat(book.remaining()).isZero();
    assertThatThrownBy(book::allocate).isInstanceOf(BusinessRuleException.class);
    assertThat(book.overlaps(100, 200)).isTrue();
    assertThat(book.overlaps(101, 200)).isFalse();
    assertThatThrownBy(() -> new ChequeBook(1L, 10, 5, LocalDate.of(2026, 1, 1)))
        .isInstanceOf(BusinessRuleException.class);
    ChequeBook other = new ChequeBook(1L, 1, 5, LocalDate.of(2026, 1, 1));
    other.cancel();
    assertThat(other.remaining()).isZero();
  }

  @Test
  void pettyCashFundNeverExceedsImprestNorGoesNegative() {
    PettyCashFund fund = new PettyCashFund(1L, 1L, "PCF-T", "PHP");
    fund.changeImprest(new BigDecimal("1000.00"));
    assertThatThrownBy(() -> fund.disburse(BigDecimal.ONE))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> fund.establish(LocalDate.of(2026, 1, 1)))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void paymentCategoryFollowsDocumentsAndPartyTypes() {
    assertThat(PaymentCategory.defaultFor(PartyType.GARAGE, Set.of("SUPPLIER_INVOICE")))
        .isEqualTo(PaymentCategory.SUPPLIER);
    assertThat(PaymentCategory.defaultFor(PartyType.GARAGE, Set.of("CLAIM_SETTLEMENT")))
        .isEqualTo(PaymentCategory.CLAIM);
    assertThat(PaymentCategory.defaultFor(PartyType.BROKER, Set.of()))
        .isEqualTo(PaymentCategory.COMMISSION);
    assertThat(PaymentCategory.defaultFor(PartyType.REINSURER, Set.of("RI_STATEMENT")))
        .isEqualTo(PaymentCategory.REINSURANCE);
    assertThat(PaymentCategory.defaultFor(PartyType.INDIVIDUAL_CLIENT, Set.of("CREDIT_NOTE")))
        .isEqualTo(PaymentCategory.PREMIUM_REFUND);
    assertThat(PaymentCategory.defaultFor(PartyType.CORPORATE_CLIENT, Set.of("CLAIM_PAYABLE")))
        .isEqualTo(PaymentCategory.CLAIM);
    assertThat(PaymentCategory.defaultFor(PartyType.SUPPLIER, Set.of()))
        .isEqualTo(PaymentCategory.SUPPLIER);
    assertThat(PaymentCategory.defaultFor(PartyType.COINSURER, Set.of())).isNull();
    assertThat(PaymentCategory.CLAIM.eventType()).isEqualTo("CLAIM_PAYMENT");
    assertThat(PaymentCategory.REINSURANCE.accepts(PartyType.SUPPLIER)).isFalse();
    assertThat(PaymentMode.BANK_TRANSFER.usesCheque()).isFalse();
    assertThat(PaymentMode.PDC.usesCheque()).isTrue();
    assertThat(IssuedPdcStatus.DUE.isOutstanding()).isTrue();
    assertThat(IssuedPdcStatus.PRESENTED.isOutstanding()).isFalse();
  }
}
