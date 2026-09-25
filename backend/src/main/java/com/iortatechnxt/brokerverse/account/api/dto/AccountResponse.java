package com.iortatechnxt.brokerverse.account.api.dto;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountContact;
import com.iortatechnxt.brokerverse.account.domain.AccountPremium;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.FreeFirstYear;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.SalesStamp;
import com.iortatechnxt.brokerverse.account.domain.TsuClearance;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * An account with its items, premium, tags and lifecycle.
 *
 * @param id id
 * @param companyId company
 * @param arn Account Reference Number
 * @param quotationRef quotation reference
 * @param proposalRef proposal request reference
 * @param clientId client
 * @param clientCode client code
 * @param clientName client name
 * @param productCode product
 * @param lineCode product line
 * @param coverTypeCode cover type
 * @param marketSegment market segment
 * @param sourceChannel source channel
 * @param insurerCode insurer
 * @param insurerBranch insurer branch
 * @param periodFrom period start
 * @param periodTo period end
 * @param multiYear multi-year
 * @param termYears term in years
 * @param currency currency
 * @param totalSumInsured total sum insured
 * @param premium premium breakdown
 * @param paymentArrangement payment arrangement
 * @param directPayment direct payment flag (BRNB.114)
 * @param directPaymentTaggedBy who tagged direct payment
 * @param mortgageeBank mortgagee bank
 * @param loanApplicationNo loan application number
 * @param pnNumbers promissory note numbers
 * @param freeFirstYear Free First Year tag
 * @param contact account contact
 * @param sales sales unit and cost center
 * @param status status
 * @param tsu TSU clearance
 * @param directBooking booked directly
 * @param lifecycle lifecycle data
 * @param policyNumbers policy numbers
 * @param items risk items
 * @param createdBy creator
 * @param createdAt creation time
 * @param productVersionNo package version that priced the premium (BRPM.007), null when none
 * @param rateOverrideRef approved rate-scheme exception used, null when none
 */
public record AccountResponse(
    Long id,
    Long companyId,
    String arn,
    String quotationRef,
    String proposalRef,
    Long clientId,
    String clientCode,
    String clientName,
    String productCode,
    String lineCode,
    String coverTypeCode,
    String marketSegment,
    String sourceChannel,
    String insurerCode,
    String insurerBranch,
    LocalDate periodFrom,
    LocalDate periodTo,
    boolean multiYear,
    int termYears,
    String currency,
    BigDecimal totalSumInsured,
    AccountPremium premium,
    PaymentArrangement paymentArrangement,
    boolean directPayment,
    String directPaymentTaggedBy,
    String mortgageeBank,
    String loanApplicationNo,
    List<String> pnNumbers,
    FreeFirstYear freeFirstYear,
    AccountContact contact,
    SalesStamp sales,
    AccountStatus status,
    TsuClearance tsu,
    boolean directBooking,
    LifecycleResponse lifecycle,
    List<String> policyNumbers,
    List<ItemResponse> items,
    String createdBy,
    Instant createdAt,
    Integer productVersionNo,
    String rateOverrideRef) {

  /**
   * Maps an account (items and numbers loaded).
   *
   * @param a account
   * @return response
   */
  public static AccountResponse from(Account a) {
    return new AccountResponse(
        a.getId(),
        a.getCompanyId(),
        a.getArn(),
        a.getQuotationRef(),
        a.getProposalRef(),
        a.getClientId(),
        a.getClientCode(),
        a.getClientName(),
        a.getProductCode(),
        a.getLineCode(),
        a.getCoverTypeCode(),
        a.getMarketSegment(),
        a.getSourceChannel(),
        a.getInsurerCode(),
        a.getInsurerBranch(),
        a.getPeriodFrom(),
        a.getPeriodTo(),
        a.isMultiYear(),
        a.getTermYears(),
        a.getCurrency(),
        a.getTotalSumInsured(),
        a.getPremium(),
        a.getPaymentArrangement(),
        a.isDirectPayment(),
        a.getDirectPaymentTaggedBy(),
        a.getMortgageeBank(),
        a.getLoanApplicationNo(),
        a.getPnNumbers(),
        a.getFreeFirstYear(),
        a.getContact(),
        a.getSales(),
        a.getStatus(),
        a.getTsu(),
        a.isDirectBooking(),
        LifecycleResponse.from(a.getLifecycle()),
        a.getPolicyNumbers(),
        a.getItems().stream().map(ItemResponse::from).toList(),
        a.getCreatedBy(),
        a.getCreatedAt(),
        a.getProductVersionNo(),
        a.getRateOverrideRef());
  }
}
