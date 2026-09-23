package com.iortatechnxt.finverse.reinsurance.service;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.reinsurance.domain.CessionLineRepository;
import com.iortatechnxt.finverse.reinsurance.domain.RiClaimShareRepository;
import com.iortatechnxt.finverse.reinsurance.domain.Soa;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Settles a statement of account against the reinsurer's open items.
 *
 * <p>The statement's items are the open items recorded for its cessions, claim recoveries and
 * statement postings. Their outstanding amounts give P (net due to the reinsurer on "due to
 * reinsurers": premium less commission, levy and reserves, plus interest) and R (net recoveries due
 * from it on "amounts recoverable"). The balance P − R is paid ({@code RI_SETTLEMENT_PAYMENT}) or
 * received ({@code RI_SETTLEMENT_RECEIPT}); the smaller side is set off between the two control
 * accounts ({@code RI_BALANCE_OFFSET}); finally every debit item is matched against the credit
 * items. Items already settled elsewhere (payables, receivables) are simply not outstanding.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class SoaSettlement {

  private final CessionLineRepository lines;
  private final RiClaimShareRepository shares;
  private final OpenItemService openItems;
  private final ReinsuranceAccounting accounting;

  /**
   * Creates the component.
   *
   * @param lines cession posting references
   * @param shares recovery posting references
   * @param openItems party sub-ledger
   * @param accounting reinsurance postings
   */
  public SoaSettlement(
      CessionLineRepository lines,
      RiClaimShareRepository shares,
      OpenItemService openItems,
      ReinsuranceAccounting accounting) {
    this.lines = lines;
    this.shares = shares;
    this.openItems = openItems;
    this.accounting = accounting;
  }

  /**
   * Outstanding open items of a statement.
   *
   * @param soa statement
   * @return items not yet settled
   */
  public List<OpenItem> outstandingItems(Soa soa) {
    Set<String> refs = new HashSet<>();
    Long treatyId = soa.getTreaty().getId();
    Long partyId = soa.getParty().getId();
    refs.addAll(lines.postingRefs(treatyId, partyId, soa.getPeriodFrom(), soa.getPeriodTo()));
    refs.addAll(shares.recoveryRefs(treatyId, partyId, soa.getPeriodFrom(), soa.getPeriodTo()));
    String own = "RI:SOA:" + soa.getSoaNo();
    refs.add(own);
    refs.add(own + ":PREM");
    return openItems.partyItems(soa.getCompanyId(), partyId).stream()
        .filter(i -> ReinsuranceAccounting.MODULE.equals(i.getSourceModule()))
        .filter(i -> refs.contains(i.getSourceReference()))
        .filter(i -> i.outstanding().signum() > 0)
        .toList();
  }

  /**
   * Settles a statement.
   *
   * @param soa approved statement
   * @param ctx posting context (settlement date, head office)
   * @param bankAccount bank account code
   * @return journal batch of the payment or receipt, or of the set-off when the balance is nil
   */
  public String settle(Soa soa, PostingContext ctx, String bankAccount) {
    List<OpenItem> items = new ArrayList<>(outstandingItems(soa));
    if (items.isEmpty()) {
      throw new BusinessRuleException(
          "NOTHING_TO_SETTLE", "Statement " + soa.getSoaNo() + " has no outstanding items");
    }
    BigDecimal premiumSide = side(items, false);
    BigDecimal recoverySide = side(items, true).negate();
    BigDecimal net = premiumSide.subtract(recoverySide);
    Long partyId = soa.getParty().getId();
    String ref = "RI:SOA:" + soa.getSoaNo();
    String batch = null;
    if (net.signum() != 0) {
      ReinsuranceAccounting.Settlement s =
          accounting.settle(ctx, ref + ":SETTLE", partyId, net, bankAccount);
      items.add(s.item());
      batch = s.batchNo();
    }
    BigDecimal offset = net.signum() >= 0 ? recoverySide : premiumSide;
    if (offset.signum() != 0) {
      String offsetBatch = accounting.offset(ctx, ref + ":OFFSET", partyId, offset);
      batch = batch == null ? offsetBatch : batch;
    }
    match(items, ctx);
    return batch;
  }

  /**
   * Outstanding balance of the premium side (due to reinsurers) or of the recovery side (amounts
   * recoverable), signed CREDIT positive.
   */
  private static BigDecimal side(List<OpenItem> items, boolean recoveries) {
    return items.stream()
        .filter(
            i ->
                i.getDocumentType().startsWith(ReinsuranceAccounting.RECOVERY_DOCUMENT)
                    == recoveries)
        .map(
            i ->
                i.getDirection() == ItemDirection.CREDIT
                    ? i.outstanding()
                    : i.outstanding().negate())
        .reduce(Money.zero(), BigDecimal::add);
  }

  private void match(List<OpenItem> items, PostingContext ctx) {
    List<OpenItem> debits =
        items.stream().filter(i -> i.getDirection() == ItemDirection.DEBIT).toList();
    List<OpenItem> credits =
        items.stream().filter(i -> i.getDirection() == ItemDirection.CREDIT).toList();
    for (OpenItem debit : debits) {
      for (OpenItem credit : credits) {
        BigDecimal value = debit.outstanding().min(credit.outstanding());
        if (value.signum() > 0) {
          openItems.match(debit.getId(), credit.getId(), value, ctx.date());
        }
      }
    }
  }
}
