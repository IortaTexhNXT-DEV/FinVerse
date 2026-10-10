package com.iortatechnxt.brokerverse.cashiering.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptStatus;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSearchService.PrintFilter;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSearchService.ReceiptCriteria;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** The criteria of a receipt search are logged in words: labels and dd-MMM-yyyy dates, no codes. */
class ReceiptCriteriaTextTest {

  @Test
  void theCriteriaReadAsLabelsAndFormattedDates() {
    ReceiptCriteria criteria =
        new ReceiptCriteria(
            1L,
            null,
            null,
            null,
            "santos",
            null,
            new BigDecimal("1500.00"),
            LocalDate.of(2026, 10, 1),
            null,
            null,
            null,
            ReceiptKind.AR,
            ReceiptStatus.ISSUED,
            new PrintFilter(3L, Boolean.FALSE, false, null));
    assertThat(criteria.describe())
        .isEqualTo(
            "Payor: santos; Amount: 1500.00; Issued from: 01-Oct-2026; Receipt type: AR;"
                + " Status: Issued; Receipting branch: 3; Printed: No")
        .doesNotContain("ISSUED", "2026-10");
  }

  @Test
  void noCriteriaMeansAllReceipts() {
    ReceiptCriteria criteria =
        new ReceiptCriteria(
            1L, null, null, null, null, null, null, null, null, null, null, null, null, null);
    assertThat(criteria.describe()).isEqualTo("All receipts");
  }
}
