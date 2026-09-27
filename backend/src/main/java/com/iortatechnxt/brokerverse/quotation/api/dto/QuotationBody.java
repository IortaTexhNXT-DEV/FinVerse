package com.iortatechnxt.brokerverse.quotation.api.dto;

import com.iortatechnxt.brokerverse.account.api.dto.ItemRequest;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDraft;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDraft.DraftItem;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDraft.Terms;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * A quotation as entered on the wizard (create, update and live premium).
 *
 * @param companyId company
 * @param clientId client or prospect
 * @param productCode risk code
 * @param marketSegment market segment
 * @param sourceChannel source channel
 * @param requestId quotation request answered
 * @param currency currency
 * @param insurerCode insurer party code
 * @param insurerBranch insurer branch
 * @param periodFrom period start
 * @param periodTo period end
 * @param validUntil validity (default when empty)
 * @param directPayment premium paid directly to the insurer (MKTID.011)
 * @param ratingBasis ANNUAL, PRO_RATA or SHORT_PERIOD
 * @param remarks remarks
 * @param items risk items with their risk group
 */
public record QuotationBody(
    @NotNull Long companyId,
    Long clientId,
    @Size(max = 20) String productCode,
    @Size(max = 40) String marketSegment,
    @Size(max = 40) String sourceChannel,
    Long requestId,
    @Size(max = 3) String currency,
    @Size(max = 30) String insurerCode,
    @Size(max = 20) String insurerBranch,
    LocalDate periodFrom,
    LocalDate periodTo,
    LocalDate validUntil,
    boolean directPayment,
    @Size(max = 20) String ratingBasis,
    @Size(max = 2000) String remarks,
    @Size(max = 200) List<@Valid Item> items) {

  /**
   * A risk item and its risk group.
   *
   * @param riskGroup risk group (account), 1 when empty
   * @param item risk data
   */
  public record Item(@Min(1) @Max(99) Integer riskGroup, @NotNull @Valid ItemRequest item) {}

  /**
   * As a draft.
   *
   * @return draft
   */
  public QuotationDraft draft() {
    return new QuotationDraft(
        clientId,
        productCode,
        marketSegment,
        sourceChannel,
        requestId,
        currency,
        new Terms(
            insurerCode,
            insurerBranch,
            periodFrom,
            periodTo,
            validUntil,
            directPayment,
            ratingBasis,
            remarks),
        items == null
            ? List.of()
            : items.stream().map(i -> new DraftItem(i.riskGroup(), i.item().data())).toList());
  }
}
