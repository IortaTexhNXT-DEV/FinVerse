package com.iortatechnxt.finverse.reinsurance.service;

import com.iortatechnxt.finverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.finverse.accounting.service.BusinessEvent;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.party.service.PartyService;
import com.iortatechnxt.finverse.reinsurance.domain.SoaFigures;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import com.iortatechnxt.finverse.subledger.domain.OpenItemValues;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accounts for reinsurance transactions inside the caller's transaction: publishes the accounting
 * event (the engine picks the GL accounts) and records the reinsurer's open item. All reinsurance
 * postings are in the company base currency.
 *
 * <table>
 *   <caption>Events and open items</caption>
 *   <tr><th>Event</th><th>Components</th><th>Open item</th></tr>
 *   <tr><td>RI_PREMIUM_CEDED</td><td>CEDED_PREMIUM, RI_COMMISSION, NET_DUE</td>
 *       <td>CREDIT REINSURANCE_PREMIUM (DEBIT ..._RETURN when negative)</td></tr>
 *   <tr><td>RI_CLAIM_RECOVERY</td><td>RECOVERY</td>
 *       <td>DEBIT REINSURANCE_RECOVERY (CREDIT ..._RETURN when negative)</td></tr>
 *   <tr><td>RI_RESERVE_SHARE</td><td>RESERVE_CHANGE</td><td>none</td></tr>
 *   <tr><td>RI_SOA_ADJUSTMENT</td><td>LEVY, PREMIUM_RESERVE, LOSS_RESERVE, INTEREST</td>
 *       <td>DEBIT REINSURANCE_SOA_ADJUSTMENT (CREDIT when negative)</td></tr>
 *   <tr><td>RI_SETTLEMENT_PAYMENT / _RECEIPT</td><td>AMOUNT</td>
 *       <td>DEBIT REINSURANCE_PAYMENT / CREDIT REINSURANCE_RECEIPT</td></tr>
 *   <tr><td>RI_BALANCE_OFFSET</td><td>AMOUNT</td><td>none (items matched)</td></tr>
 * </table>
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ReinsuranceAccounting {

  /** Source module recorded on events, journals and open items. */
  public static final String MODULE = "REINSURANCE";

  /** Open item document type of premium due to a reinsurer. */
  public static final String PREMIUM_DOCUMENT = "REINSURANCE_PREMIUM";

  /** Open item document type of a recovery due from a reinsurer. */
  public static final String RECOVERY_DOCUMENT = "REINSURANCE_RECOVERY";

  private static final String RETURN_SUFFIX = "_RETURN";
  private static final String AMOUNT = "AMOUNT";

  private final AccountingEventPublisher publisher;
  private final OpenItemService openItems;
  private final PartyService parties;
  private final OrganizationService organization;

  /**
   * Creates the service.
   *
   * @param publisher accounting engine
   * @param openItems party sub-ledger
   * @param parties reinsurers
   * @param organization company base currency
   */
  public ReinsuranceAccounting(
      AccountingEventPublisher publisher,
      OpenItemService openItems,
      PartyService parties,
      OrganizationService organization) {
    this.publisher = publisher;
    this.openItems = openItems;
    this.parties = parties;
    this.organization = organization;
  }

  /**
   * Premium ceded to a reinsurer, net of its commission.
   *
   * @param ctx posting context
   * @param ref unique source reference
   * @param partyId reinsurer
   * @param premium base currency premium (negative for return premium)
   * @param commission base currency commission
   * @return journal batch number, null when nothing was posted
   */
  public String cede(
      PostingContext ctx, String ref, Long partyId, BigDecimal premium, BigDecimal commission) {
    if (premium.signum() == 0 && commission.signum() == 0) {
      return null;
    }
    Party party = parties.get(partyId);
    BigDecimal netDue = premium.subtract(commission);
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    amounts.put("CEDED_PREMIUM", premium);
    amounts.put("RI_COMMISSION", commission);
    amounts.put("NET_DUE", netDue);
    String batch = publish("RI_PREMIUM_CEDED", ctx, ref, party.getCode(), amounts, Map.of());
    record(ctx, party, ref, batch, new ItemSpec(netDue, ItemDirection.CREDIT, PREMIUM_DOCUMENT));
    return batch;
  }

  /**
   * Recovery due from a reinsurer on a paid claim (negative when a salvage is shared back).
   *
   * @param ctx posting context
   * @param ref unique source reference
   * @param partyId reinsurer
   * @param amount base currency amount
   * @return journal batch number
   */
  public String recovery(PostingContext ctx, String ref, Long partyId, BigDecimal amount) {
    Party party = parties.get(partyId);
    String batch =
        publish(
            "RI_CLAIM_RECOVERY", ctx, ref, party.getCode(), Map.of("RECOVERY", amount), Map.of());
    record(ctx, party, ref, batch, new ItemSpec(amount, ItemDirection.DEBIT, RECOVERY_DOCUMENT));
    return batch;
  }

  /**
   * Change of the reinsurers' share of outstanding claims (asset, no party).
   *
   * @param ctx posting context
   * @param ref unique source reference
   * @param amount base currency change (negative = release)
   * @return journal batch number
   */
  public String reserveShare(PostingContext ctx, String ref, BigDecimal amount) {
    return publish("RI_RESERVE_SHARE", ctx, ref, null, Map.of("RESERVE_CHANGE", amount), Map.of());
  }

  /**
   * Statement adjustments withheld from (or credited to) a reinsurer on approval.
   *
   * @param ctx posting context
   * @param ref unique source reference
   * @param partyId reinsurer
   * @param f statement figures
   * @return journal batch number, null when there is nothing to post
   */
  public String statementAdjustments(PostingContext ctx, String ref, Long partyId, SoaFigures f) {
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    amounts.put("LEVY", f.levy());
    amounts.put("PREMIUM_RESERVE", f.premiumReserveNet());
    amounts.put("LOSS_RESERVE", f.lossReserveNet());
    amounts.put("INTEREST", f.interest());
    if (amounts.values().stream().allMatch(v -> v.signum() == 0)) {
      return null;
    }
    Party party = parties.get(partyId);
    String batch = publish("RI_SOA_ADJUSTMENT", ctx, ref, party.getCode(), amounts, Map.of());
    record(
        ctx,
        party,
        ref,
        batch,
        new ItemSpec(f.adjustments(), ItemDirection.DEBIT, "REINSURANCE_SOA_ADJUSTMENT"));
    return batch;
  }

  /**
   * Payment to (positive) or receipt from (negative) a reinsurer, recorded as an open item to be
   * matched against the settled items.
   *
   * @param ctx posting context
   * @param ref unique source reference
   * @param partyId reinsurer
   * @param net amount paid (positive) or received (negative)
   * @param bankAccount bank account code
   * @return the settlement open item
   */
  public Settlement settle(
      PostingContext ctx, String ref, Long partyId, BigDecimal net, String bankAccount) {
    Party party = parties.get(partyId);
    boolean payment = net.signum() > 0;
    String batch =
        publish(
            payment ? "RI_SETTLEMENT_PAYMENT" : "RI_SETTLEMENT_RECEIPT",
            ctx,
            ref,
            party.getCode(),
            Map.of(AMOUNT, net.abs()),
            Map.of("BANK", bankAccount));
    OpenItem item =
        record(
            ctx,
            party,
            ref,
            batch,
            payment
                ? new ItemSpec(net, ItemDirection.DEBIT, "REINSURANCE_PAYMENT")
                : new ItemSpec(net.negate(), ItemDirection.CREDIT, "REINSURANCE_RECEIPT"));
    return new Settlement(batch, item);
  }

  /**
   * Sets recoveries due from a reinsurer off against premium due to it (both control accounts of
   * the same party); the open items are matched by the caller.
   *
   * @param ctx posting context
   * @param ref unique source reference
   * @param partyId reinsurer
   * @param amount amount set off (negative reverses)
   * @return journal batch number
   */
  public String offset(PostingContext ctx, String ref, Long partyId, BigDecimal amount) {
    Party party = parties.get(partyId);
    return publish(
        "RI_BALANCE_OFFSET", ctx, ref, party.getCode(), Map.of(AMOUNT, amount), Map.of());
  }

  private String publish(
      String eventType,
      PostingContext ctx,
      String ref,
      String partyCode,
      Map<String, BigDecimal> amounts,
      Map<String, String> accounts) {
    return publisher
        .publish(
            new BusinessEvent(
                eventType,
                ctx.companyId(),
                ctx.branchId(),
                ctx.date(),
                baseCurrency(ctx.companyId()),
                MODULE,
                ref,
                ctx.documentNo(),
                partyCode,
                ctx.businessLine(),
                null,
                ctx.narration(),
                amounts,
                accounts))
        .getBatchNo();
  }

  private OpenItem record(
      PostingContext ctx, Party party, String ref, String batch, ItemSpec spec) {
    if (spec.amount().signum() == 0) {
      return null;
    }
    boolean positive = spec.amount().signum() > 0;
    BigDecimal amount = Money.round(spec.amount().abs());
    return openItems.record(
        new OpenItemValues(
            ctx.companyId(),
            ctx.branchId(),
            party.getId(),
            party.getCode(),
            positive ? spec.direction() : spec.direction().opposite(),
            positive ? spec.documentType() : spec.documentType() + RETURN_SUFFIX,
            ctx.documentNo(),
            ctx.date(),
            ctx.date().plusDays(party.getCreditDays()),
            baseCurrency(ctx.companyId()),
            amount,
            amount,
            MODULE,
            ref,
            batch,
            ctx.narration()));
  }

  private String baseCurrency(Long companyId) {
    return organization.getCompany(companyId).getBaseCurrency();
  }

  /**
   * Result of a settlement posting.
   *
   * @param batchNo journal batch number
   * @param item settlement open item
   */
  public record Settlement(String batchNo, OpenItem item) {}

  /**
   * Open item to record.
   *
   * @param amount signed amount (negative flips direction and uses the _RETURN type)
   * @param direction direction of a positive amount
   * @param documentType document type of a positive amount
   */
  private record ItemSpec(BigDecimal amount, ItemDirection direction, String documentType) {}
}
