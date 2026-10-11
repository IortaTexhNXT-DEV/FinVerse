package com.iortatechnxt.brokerverse.account.api.dto;

import com.iortatechnxt.brokerverse.account.domain.AccountContact;
import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * New or changed account (wizard, BRNB.051/053/054).
 *
 * @param companyId company (used on creation)
 * @param clientId client
 * @param productCode risk code
 * @param marketSegment market segment
 * @param sourceChannel source channel
 * @param insurerCode insurer, may be empty until placement
 * @param insurerBranch insurer branch
 * @param periodFrom period start
 * @param periodTo period end
 * @param multiYear multi-year
 * @param termYears term in years
 * @param currency currency
 * @param paymentArrangement payment arrangement
 * @param mortgageeBank mortgagee bank
 * @param loanApplicationNo loan application number
 * @param pnNumbers promissory note numbers
 * @param contactName contact person
 * @param contactEmail contact e-mail
 * @param contactMobile contact mobile
 * @param contactAddress contact address
 * @param items risk items
 * @param ratingBasis premium period basis
 * @param commissionRate commission override %
 * @param ffyStart Free First Year start
 */
public record AccountRequest(
    @NotNull Long companyId,
    @NotNull Long clientId,
    @NotNull @Size(max = 20) String productCode,
    @Size(max = 40) String marketSegment,
    @Size(max = 40) String sourceChannel,
    @Size(max = 30) String insurerCode,
    @Size(max = 20) String insurerBranch,
    LocalDate periodFrom,
    LocalDate periodTo,
    boolean multiYear,
    @Min(1) @Max(10) int termYears,
    @Pattern(regexp = "[A-Z]{3}") String currency,
    PaymentArrangement paymentArrangement,
    @Size(max = 40) String mortgageeBank,
    @Size(max = 40) String loanApplicationNo,
    @Size(max = 20) List<@Size(max = 40) String> pnNumbers,
    @Size(max = 200) String contactName,
    @Email @Size(max = 120) String contactEmail,
    @Size(max = 30) String contactMobile,
    @Size(max = 300) String contactAddress,
    @Size(max = 500) List<@Valid ItemRequest> items,
    PeriodBasis ratingBasis,
    @DecimalMin("0") @DecimalMax("100") BigDecimal commissionRate,
    LocalDate ffyStart) {

  /**
   * As a draft.
   *
   * @return draft
   */
  public AccountDraft draft() {
    String insurer = ItemRequest.blankToNull(insurerCode);
    return new AccountDraft(
        clientId,
        productCode,
        ItemRequest.blankToNull(marketSegment),
        ItemRequest.blankToNull(sourceChannel),
        insurer,
        insurer == null ? null : ItemRequest.blankToNull(insurerBranch),
        periodFrom,
        periodTo,
        multiYear,
        termYears,
        currency,
        paymentArrangement,
        new Mortgage(
            ItemRequest.blankToNull(mortgageeBank),
            ItemRequest.blankToNull(loanApplicationNo),
            pnNumbers == null ? List.of() : pnNumbers),
        new AccountContact(
            ItemRequest.blankToNull(contactName),
            ItemRequest.blankToNull(contactEmail),
            ItemRequest.blankToNull(contactMobile),
            ItemRequest.blankToNull(contactAddress)),
        items == null ? List.of() : items.stream().map(ItemRequest::data).toList(),
        ratingBasis,
        commissionRate,
        ffyStart);
  }
}
