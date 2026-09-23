package com.iortatechnxt.finverse.payables.service;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.payables.domain.AllocationValues;
import com.iortatechnxt.finverse.payables.domain.PaymentVoucherRepository;
import com.iortatechnxt.finverse.payables.domain.VoucherAllocation;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Checks that the open items selected on a payment voucher can be paid: CREDIT items of the payee,
 * in the payment currency, not paid beyond their balance (net of amounts already reserved by other
 * draft or pending vouchers).
 */
@Component
public class PayableItemGuard {

  private static final Long NO_VOUCHER = -1L;

  private final OpenItemService openItems;
  private final PaymentVoucherRepository vouchers;

  /**
   * Creates the guard.
   *
   * @param openItems sub-ledger
   * @param vouchers voucher repository
   */
  public PayableItemGuard(OpenItemService openItems, PaymentVoucherRepository vouchers) {
    this.openItems = openItems;
    this.vouchers = vouchers;
  }

  /**
   * Amount of an item still available for a new payment.
   *
   * @param item open item
   * @param voucherId voucher being edited, null for a new one
   * @return outstanding minus amounts reserved by other live vouchers
   */
  public BigDecimal available(OpenItem item, Long voucherId) {
    BigDecimal reserved =
        vouchers.reservedAmount(item.getId(), voucherId == null ? NO_VOUCHER : voucherId);
    return item.outstanding().subtract(reserved).max(BigDecimal.ZERO);
  }

  /**
   * Builds allocations from the requested item payments.
   *
   * @param party payee
   * @param currency payment currency
   * @param requests requested items
   * @param voucherId voucher being edited, null for a new one
   * @return allocations
   */
  public List<AllocationValues> allocations(
      Party party, String currency, List<PaymentCommand.ItemPayment> requests, Long voucherId) {
    List<AllocationValues> result = new ArrayList<>();
    for (PaymentCommand.ItemPayment r : requests) {
      OpenItem item = requirePayable(openItems.get(r.openItemId()), party.getId(), currency);
      BigDecimal available = available(item, voucherId);
      BigDecimal amount = r.amount() == null ? available : r.amount();
      if (amount.signum() <= 0 || amount.compareTo(available) > 0) {
        throw new BusinessRuleException(
            "PAYMENT_EXCEEDS_BALANCE",
            "Amount "
                + amount
                + " for "
                + item.getDocumentNo()
                + " exceeds the available "
                + available);
      }
      result.add(
          new AllocationValues(
              item.getId(),
              item.getDocumentType(),
              item.getDocumentNo(),
              item.getDocumentDate(),
              item.getDueDate(),
              amount));
    }
    return result;
  }

  /**
   * Re-checks the allocations of a voucher at approval (items may have been paid meanwhile).
   *
   * @param partyId payee
   * @param currency payment currency
   * @param allocations allocations
   */
  public void revalidate(Long partyId, String currency, List<VoucherAllocation> allocations) {
    for (VoucherAllocation a : allocations) {
      OpenItem item = requirePayable(openItems.get(a.getOpenItemId()), partyId, currency);
      if (a.getAmount().compareTo(item.outstanding()) > 0) {
        throw new BusinessRuleException(
            "PAYMENT_EXCEEDS_BALANCE",
            item.getDocumentNo() + " now has only " + item.outstanding() + " outstanding");
      }
    }
  }

  private static OpenItem requirePayable(OpenItem item, Long partyId, String currency) {
    if (!Objects.equals(item.getPartyId(), partyId)
        || item.getDirection() != ItemDirection.CREDIT
        || item.outstanding().signum() <= 0) {
      throw new BusinessRuleException(
          "NOT_PAYABLE", "Item " + item.getDocumentNo() + " is not an open payable of the payee");
    }
    if (!item.getCurrency().equals(currency)) {
      throw new BusinessRuleException(
          "CURRENCY_MISMATCH",
          "Item "
              + item.getDocumentNo()
              + " is in "
              + item.getCurrency()
              + ", payment in "
              + currency);
    }
    return item;
  }
}
