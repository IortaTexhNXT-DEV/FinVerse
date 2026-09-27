package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.AccountContact;
import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Account data as entered on the wizard, a bulk row or a quotation (before the client and product
 * are resolved). Blank values are allowed in a draft; the minimum-field matrix is enforced when the
 * account is submitted.
 *
 * @param clientId crm client id
 * @param productCode risk code
 * @param marketSegment market segment (list MARKET_SEGMENT)
 * @param sourceChannel source channel (list SOURCE_CHANNEL)
 * @param insurerCode insurer party code, may be empty until placement
 * @param insurerBranch insurer branch (LGT)
 * @param periodFrom period start
 * @param periodTo period end
 * @param multiYear multi-year account (BRNB.112)
 * @param termYears term in years (multi-year)
 * @param currency currency; PHP when empty
 * @param paymentArrangement via BDOI or direct to insurer (BRNB.114)
 * @param mortgage mortgagee bank, loan application and PN numbers
 * @param contact account contact; defaulted from the client when empty (BRNB.109)
 * @param items risk items
 * @param ratingBasis premium period basis; annual when empty
 * @param commissionRate commission override in percent
 * @param ffyStart Free First Year start (BRNB.113), null for none
 */
public record AccountDraft(
    Long clientId,
    String productCode,
    String marketSegment,
    String sourceChannel,
    String insurerCode,
    String insurerBranch,
    LocalDate periodFrom,
    LocalDate periodTo,
    boolean multiYear,
    int termYears,
    String currency,
    PaymentArrangement paymentArrangement,
    Mortgage mortgage,
    AccountContact contact,
    List<RiskItemData> items,
    PeriodBasis ratingBasis,
    BigDecimal commissionRate,
    LocalDate ffyStart) {

  /** Defensive copy. */
  public AccountDraft {
    items = items == null ? List.of() : List.copyOf(items);
  }
}
