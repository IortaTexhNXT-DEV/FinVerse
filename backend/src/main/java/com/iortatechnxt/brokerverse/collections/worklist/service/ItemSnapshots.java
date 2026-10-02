package com.iortatechnxt.brokerverse.collections.worklist.service;

import com.iortatechnxt.brokerverse.collections.common.domain.AgingBrackets;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.InvoiceCategory;
import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.ItemStatus;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem.Snapshot;
import com.iortatechnxt.brokerverse.collections.common.domain.ItemParts.Balance;
import com.iortatechnxt.brokerverse.collections.common.domain.ItemParts.Classification;
import com.iortatechnxt.brokerverse.collections.common.domain.ItemParts.Figures;
import com.iortatechnxt.brokerverse.collections.common.domain.ItemParts.Parties;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceData;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The refresh rules of the worklist (BRCLXN.001-015, 046) as pure functions of a ledger invoice:
 * its snapshot (keys, classification, figures and balance per PR component) and its status.
 *
 * <ul>
 *   <li>A total to collect (premium receivable and PR2307) above the threshold is OPEN (005, 009).
 *   <li>Zero or below the threshold is COMPLETED (008); a new invoice is then not listed. So is an
 *       invoice without a client receivable (direct payment, BRNB.114: payment status
 *       NOT_APPLICABLE).
 *   <li>A negative total of a cancellation or return invoice (kind or flag CANCELLED) is
 *       EXCLUDED_CANCELLED and never listed (010); other negatives go to the credit view (CQ04).
 * </ul>
 */
final class ItemSnapshots {

  private ItemSnapshots() {}

  /**
   * The status of an invoice by the refresh rules.
   *
   * @param invoice ledger invoice
   * @param total total to collect
   * @param threshold listing threshold
   * @return status
   */
  static ItemStatus statusOf(OpsInvoice invoice, BigDecimal total, BigDecimal threshold) {
    if (total.signum() < 0) {
      return invoice.getKind().isNegative() || invoice.isCancelled()
          ? ItemStatus.EXCLUDED_CANCELLED
          : ItemStatus.CREDIT;
    }
    boolean receivable = invoice.getPaymentStatus() != PaymentStatus.NOT_APPLICABLE;
    return receivable && total.compareTo(threshold) > 0 ? ItemStatus.OPEN : ItemStatus.COMPLETED;
  }

  /**
   * Whether a status puts a new invoice in the worklist.
   *
   * @param status status by the rules
   * @return true for OPEN and CREDIT
   */
  static boolean listable(ItemStatus status) {
    return status == ItemStatus.OPEN || status == ItemStatus.CREDIT;
  }

  /**
   * The snapshot of an invoice.
   *
   * @param invoice ledger invoice, loaded
   * @param unitHead Unit Head of its sales unit, may be null
   * @param age age in days by the aging basis
   * @param brackets aging brackets
   * @return snapshot
   */
  static Snapshot of(OpsInvoice invoice, String unitHead, int age, AgingBrackets brackets) {
    OpsInvoiceData.Classification c = invoice.getClassification();
    Parties parties =
        new Parties(
            invoice.getArn(),
            invoice.getRootInvoiceNo(),
            invoice.getKind().name(),
            invoice.getPolicyNo(),
            invoice.getPolicyYear(),
            invoice.getClientCode(),
            invoice.getAssuredName(),
            invoice.getInsurerCode());
    Classification classification =
        new Classification(
            invoice.getBranchId(),
            c.segment(),
            c.salesUnit(),
            unitHead,
            c.aoUsername(),
            c.productLine(),
            c.currency(),
            c.bookingDate(),
            c.inceptionDate(),
            c.expiryDate(),
            invoice.isDpFlag(),
            invoice.isCwtFlag(),
            invoice.isDpFlag() ? InvoiceCategory.DIRECT_BILL : InvoiceCategory.REGULAR);
    Figures figures =
        new Figures(
            invoice.getGrossPremium(),
            invoice.premiumBalance(),
            pr2307(invoice),
            age,
            brackets.labelOf(age),
            invoice.getPaymentStatus());
    return new Snapshot(parties, classification, figures, balances(invoice));
  }

  /**
   * The PR2307 balance of an invoice.
   *
   * @param invoice invoice
   * @return balance, zero when the component is missing
   */
  static BigDecimal pr2307(OpsInvoice invoice) {
    return invoice.getComponents().stream()
        .filter(x -> x.getComponent() == LedgerComponent.PR2307)
        .map(OpsInvoiceComponent::getBalance)
        .findFirst()
        .orElse(BigDecimal.ZERO.setScale(2));
  }

  private static List<Balance> balances(OpsInvoice invoice) {
    List<Balance> out = new ArrayList<>();
    for (OpsInvoiceComponent x : invoice.getComponents()) {
      if (x.getComponent().isPremiumReceivable() || x.getComponent() == LedgerComponent.PR2307) {
        out.add(
            new Balance(
                x.getComponent(),
                x.getBooked(),
                x.getAdjusted(),
                x.netApplied(),
                x.getWrittenOff(),
                x.getBalance()));
      }
    }
    return out;
  }
}
