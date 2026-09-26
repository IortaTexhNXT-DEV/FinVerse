package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptAction.ReinstatementFields;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

/** The checks of a reinstatement request (CSHID.001/004/005). */
final class ReinstatementRules {

  private ReinstatementRules() {}

  /**
   * The amount to reinstate: the whole receipt or a partial amount up to it.
   *
   * @param receipt cancelled receipt
   * @param full full reinstatement
   * @param partial partial amount
   * @return amount
   */
  static BigDecimal amount(Receipt receipt, boolean full, BigDecimal partial) {
    BigDecimal amount = full ? receipt.getAmount() : partial;
    if (amount == null || amount.signum() <= 0 || amount.compareTo(receipt.getAmount()) > 0) {
      throw new BusinessRuleException(
          "REINSTATEMENT_AMOUNT",
          "The reinstated amount must be above zero and at most " + receipt.getAmount());
    }
    return amount;
  }

  /**
   * Checks the encoded fields of the reason group (CSHID.005).
   *
   * @param group reason group
   * @param f fields
   * @param receipt receipt
   */
  static void requireFields(String group, ReinstatementFields f, Receipt receipt) {
    boolean premium = !"DIRECT_PAYMENT".equals(group);
    if (premium && receipt.getKind() != ReceiptKind.AR) {
      throw new BusinessRuleException(
          "REINSTATEMENT_REASON_GROUP", "Premium reinstatement reasons apply to ARs");
    }
    List<String> missing = f == null ? List.of("reinstatement details") : missing(f, premium);
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          "REINSTATEMENT_FIELDS_REQUIRED",
          "Enter the " + String.join(", ", missing) + " (CSHID.005)");
    }
  }

  private static List<String> missing(ReinstatementFields f, boolean premium) {
    Stream<String> common =
        Stream.of(
            blank(f.invoiceNo(), "invoice number"),
            blank(f.documentNo(), premium ? "AR number" : "OR number"),
            blank(f.payorName(), "assured / payor"));
    Stream<String> officers =
        premium
            ? Stream.of(
                blank(f.accountOfficer(), "account officer"),
                blank(f.unitHead(), "unit head"),
                blank(f.teamLeader(), "team leader"))
            : Stream.empty();
    return Stream.concat(common, officers).filter(m -> m != null).toList();
  }

  private static String blank(String value, String label) {
    return value == null || value.isBlank() ? label : null;
  }
}
