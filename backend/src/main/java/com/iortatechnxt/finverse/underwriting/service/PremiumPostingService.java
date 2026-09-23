package com.iortatechnxt.finverse.underwriting.service;

import com.iortatechnxt.finverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.finverse.accounting.service.BusinessEvent;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.currency.domain.RateType;
import com.iortatechnxt.finverse.currency.service.CurrencyService;
import com.iortatechnxt.finverse.journal.domain.JournalBatch;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.domain.OpenItemValues;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.PostingRefs;
import com.iortatechnxt.finverse.underwriting.domain.PremiumBreakdown;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accounts for an approved policy or endorsement, inside the approval transaction:
 *
 * <ol>
 *   <li>premium event (GROSS_PREMIUM = premium billed, taxes, policy fee, TOTAL_DUE) and the
 *       client's open item: a DEBIT_NOTE, or a CREDIT_NOTE for return premium;
 *   <li>when intermediated, COMMISSION_ACCRUAL (COMMISSION, WITHHOLDING_TAX, NET_COMMISSION) and
 *       the intermediary's open item: commission payable (CREDIT), or recovery (DEBIT) on refunds;
 *   <li>when leading a coinsurance, COINSURANCE_SHARE (COINSURER_PREMIUM) and the coinsurer's open
 *       item (CREDIT, or DEBIT on refunds).
 * </ol>
 *
 * Open items carry the journal batch number that recorded them.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class PremiumPostingService {

  /** Source module recorded on events, journals and open items. */
  public static final String MODULE = "UNDERWRITING";

  private static final String COMMISSION_SUFFIX = ":COMM";
  private static final String COINSURANCE_SUFFIX = ":COINS";

  private final AccountingEventPublisher publisher;
  private final OpenItemService openItems;
  private final CurrencyService currencies;
  private final OrganizationService organization;
  private final UnderwritingNumbers numbers;

  /**
   * Creates the service.
   *
   * @param publisher accounting engine
   * @param openItems party sub-ledger
   * @param currencies exchange rates
   * @param organization company facts
   * @param numbers document numbers
   */
  public PremiumPostingService(
      AccountingEventPublisher publisher,
      OpenItemService openItems,
      CurrencyService currencies,
      OrganizationService organization,
      UnderwritingNumbers numbers) {
    this.publisher = publisher;
    this.openItems = openItems;
    this.currencies = currencies;
    this.organization = organization;
    this.numbers = numbers;
  }

  /**
   * Posts the premium, commission and coinsurance of a document.
   *
   * @param posting document to account for
   * @return accounting references to store on the document
   */
  public PostingRefs post(PremiumPosting posting) {
    Policy policy = posting.policy();
    String baseCurrency = organization.getCompany(policy.getCompanyId()).getBaseCurrency();
    BigDecimal rate =
        currencies.rateOn(baseCurrency, policy.getCurrency(), RateType.SPOT, posting.date());
    PremiumBreakdown p = posting.premium();
    String clientNote = null;
    String premiumBatch = null;
    if (p.getTotalDue().signum() != 0) {
      JournalBatch batch = publisher.publish(premiumEvent(posting));
      premiumBatch = batch.getBatchNo();
      clientNote = numbers.note(p.getTotalDue().signum() > 0, policy.getBranchId(), posting.date());
      recordItem(
          posting,
          new ItemSpec(
              policy.getCustomer(),
              p.getTotalDue(),
              ItemDirection.DEBIT,
              "DEBIT_NOTE",
              "CREDIT_NOTE",
              clientNote,
              posting.sourceReference(),
              premiumBatch),
          rate);
    }
    String intermediaryNote = null;
    String commissionBatch = null;
    if (policy.getIntermediary() != null && p.getCommission().signum() != 0) {
      commissionBatch = publisher.publish(commissionEvent(posting)).getBatchNo();
      intermediaryNote =
          numbers.note(p.getCommission().signum() < 0, policy.getBranchId(), posting.date());
      recordItem(
          posting,
          new ItemSpec(
              policy.getIntermediary(),
              p.getNetCommission(),
              ItemDirection.CREDIT,
              "COMMISSION",
              "COMMISSION_RECOVERY",
              intermediaryNote,
              posting.sourceReference() + COMMISSION_SUFFIX,
              commissionBatch),
          rate);
    }
    postCoinsurance(posting, clientNote, rate);
    return new PostingRefs(clientNote, intermediaryNote, premiumBatch, commissionBatch, rate);
  }

  private void postCoinsurance(PremiumPosting posting, String documentNo, BigDecimal rate) {
    Party coinsurer = posting.leadingCoinsurer();
    BigDecimal share = posting.premium().getCoinsurerPremium();
    if (coinsurer == null || share.signum() == 0) {
      return;
    }
    String key = posting.sourceReference() + COINSURANCE_SUFFIX;
    JournalBatch batch =
        publisher.publish(
            event(
                posting,
                "COINSURANCE_SHARE",
                key,
                coinsurer.getCode(),
                Map.of("COINSURER_PREMIUM", share)));
    recordItem(
        posting,
        new ItemSpec(
            coinsurer,
            share,
            ItemDirection.CREDIT,
            "COINSURANCE_SHARE",
            "COINSURANCE_RETURN",
            documentNo,
            key,
            batch.getBatchNo()),
        rate);
  }

  private BusinessEvent premiumEvent(PremiumPosting posting) {
    PremiumBreakdown p = posting.premium();
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    amounts.put("GROSS_PREMIUM", p.getBilledPremium());
    amounts.put("DST", p.getDst());
    amounts.put("VAT", p.getVat());
    amounts.put("LGT", p.getLgt());
    amounts.put("FST", p.getFst());
    amounts.put("PREMIUM_TAX", p.getPremiumTax());
    amounts.put("POLICY_FEE", p.getPolicyFee());
    amounts.put("TOTAL_DUE", p.getTotalDue());
    return event(
        posting,
        posting.eventType(),
        posting.sourceReference(),
        posting.policy().getCustomer().getCode(),
        amounts);
  }

  private BusinessEvent commissionEvent(PremiumPosting posting) {
    PremiumBreakdown p = posting.premium();
    return event(
        posting,
        "COMMISSION_ACCRUAL",
        posting.sourceReference() + COMMISSION_SUFFIX,
        posting.policy().getIntermediary().getCode(),
        Map.of(
            "COMMISSION", p.getCommission(),
            "WITHHOLDING_TAX", p.getWithholdingTax(),
            "NET_COMMISSION", p.getNetCommission()));
  }

  private static BusinessEvent event(
      PremiumPosting posting,
      String eventType,
      String sourceReference,
      String partyCode,
      Map<String, BigDecimal> amounts) {
    Policy policy = posting.policy();
    return new BusinessEvent(
        eventType,
        policy.getCompanyId(),
        policy.getBranchId(),
        posting.date(),
        policy.getCurrency(),
        MODULE,
        sourceReference,
        posting.reference(),
        partyCode,
        policy.getProduct().getBusinessLine(),
        null,
        posting.narration(),
        amounts,
        Map.of());
  }

  private void recordItem(PremiumPosting posting, ItemSpec spec, BigDecimal rate) {
    Policy policy = posting.policy();
    boolean positive = spec.amount().signum() > 0;
    BigDecimal amount = spec.amount().abs();
    openItems.record(
        new OpenItemValues(
            policy.getCompanyId(),
            policy.getBranchId(),
            spec.party().getId(),
            spec.party().getCode(),
            positive ? spec.direction() : spec.direction().opposite(),
            positive ? spec.documentType() : spec.reversedType(),
            spec.documentNo(),
            posting.date(),
            posting.date().plusDays(spec.party().getCreditDays()),
            policy.getCurrency(),
            amount,
            Money.convert(amount, rate),
            MODULE,
            spec.sourceReference(),
            spec.batchNo(),
            posting.narration()));
  }

  /**
   * Open item to record for one party.
   *
   * @param party party
   * @param amount signed amount (negative flips the direction)
   * @param direction direction for a positive amount
   * @param documentType document type for a positive amount
   * @param reversedType document type for a negative amount
   * @param documentNo document number
   * @param sourceReference unique key
   * @param batchNo journal batch
   */
  private record ItemSpec(
      Party party,
      BigDecimal amount,
      ItemDirection direction,
      String documentType,
      String reversedType,
      String documentNo,
      String sourceReference,
      String batchNo) {}
}
