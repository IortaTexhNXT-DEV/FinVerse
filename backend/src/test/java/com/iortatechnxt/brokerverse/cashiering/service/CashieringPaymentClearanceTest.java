package com.iortatechnxt.brokerverse.cashiering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Only a check (or a matured post-dated check) still clears; cash and channel payments do not. */
class CashieringPaymentClearanceTest {

  private final CashReceiptRepository receipts = mock(CashReceiptRepository.class);
  private final CashieringPaymentClearance clearance = new CashieringPaymentClearance(receipts);

  private void receipt(String arNo, PaymentMode mode) {
    Receipt r = mock(Receipt.class);
    when(r.getMode()).thenReturn(mode);
    when(receipts.findByReceiptNo(arNo)).thenReturn(Optional.of(r));
  }

  @Test
  void checksClearCashAndChannelPaymentsDoNot() {
    receipt("AR-HO-000001", PaymentMode.CHECK);
    receipt("AR-HO-000002", PaymentMode.PDC);
    receipt("AR-HO-000003", PaymentMode.CASH);
    receipt("AR-HO-000004", PaymentMode.BILLS_PAYMENT);
    assertThat(clearance.clears("AR-HO-000001")).isTrue();
    assertThat(clearance.clears("AR-HO-000002")).isTrue();
    assertThat(clearance.clears("AR-HO-000003")).isFalse();
    assertThat(clearance.clears("AR-HO-000004")).isFalse();
  }

  @Test
  void anUnknownReceiptIsTreatedAsACheck() {
    when(receipts.findByReceiptNo("AR-XX-999999")).thenReturn(Optional.empty());
    assertThat(clearance.clears("AR-XX-999999")).isTrue();
    assertThat(clearance.clears(null)).isTrue();
  }
}
