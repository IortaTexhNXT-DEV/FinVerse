package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentClearance;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The clearance of a payment as Cashiering knows it: the receipt's mode of payment tells whether
 * the money still clears (check, matured post-dated check) or was received at once (cash, bills
 * payment, trade, CLPC, direct credit, an insurer's settlement).
 */
@Service
@Transactional(readOnly = true)
public class CashieringPaymentClearance implements PaymentClearance {

  private static final Set<PaymentMode> CLEARING = EnumSet.of(PaymentMode.CHECK, PaymentMode.PDC);

  private final CashReceiptRepository receipts;

  /**
   * Creates the port.
   *
   * @param receipts receipts
   */
  public CashieringPaymentClearance(CashReceiptRepository receipts) {
    this.receipts = receipts;
  }

  @Override
  public boolean clears(String arNo) {
    if (arNo == null || arNo.isBlank()) {
      return true;
    }
    return receipts
        .findByReceiptNo(arNo)
        .map(Receipt::getMode)
        .map(CLEARING::contains)
        .orElse(true);
  }
}
