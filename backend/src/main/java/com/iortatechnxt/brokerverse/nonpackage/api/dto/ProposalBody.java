package com.iortatechnxt.brokerverse.nonpackage.api.dto;

import com.iortatechnxt.brokerverse.account.api.dto.ItemRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.RiskDetails;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalDraft;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * A PRF as entered (create and update).
 *
 * @param companyId company
 * @param clientId client or prospect
 * @param productCode risk code
 * @param marketSegment market segment
 * @param sourceChannel source channel
 * @param currency currency
 * @param periodFrom requested period start
 * @param periodTo requested period end
 * @param sections free-form risk sections
 * @param items risk items
 * @param insurers requested insurers
 */
public record ProposalBody(
    @NotNull Long companyId,
    Long clientId,
    @Size(max = 20) String productCode,
    @Size(max = 40) String marketSegment,
    @Size(max = 40) String sourceChannel,
    @Size(max = 3) String currency,
    LocalDate periodFrom,
    LocalDate periodTo,
    @Size(max = 30) List<@Valid SectionBody> sections,
    @Size(max = 200) List<@Valid RiskItem> items,
    @Size(max = 20) List<@Size(max = 30) String> insurers) {

  /**
   * A free-form section.
   *
   * @param heading heading
   * @param text text
   */
  public record SectionBody(@Size(max = 120) String heading, @Size(max = 4000) String text) {}

  /**
   * A risk item in its risk group.
   *
   * @param riskGroup risk group, 1 when empty
   * @param risk risk data
   */
  public record RiskItem(@Min(1) @Max(99) Integer riskGroup, @NotNull @Valid ItemRequest risk) {}

  /**
   * As a draft.
   *
   * @return draft
   */
  public ProposalDraft draft() {
    List<RiskDetails.Section> s =
        sections == null
            ? List.of()
            : sections.stream().map(x -> new RiskDetails.Section(x.heading(), x.text())).toList();
    List<RiskDetails.Item> i =
        items == null
            ? List.of()
            : items.stream()
                .map(
                    x ->
                        new RiskDetails.Item(
                            x.riskGroup() == null ? 1 : x.riskGroup(), x.risk().data()))
                .toList();
    return new ProposalDraft(
        clientId,
        productCode,
        marketSegment,
        sourceChannel,
        currency,
        periodFrom,
        periodTo,
        new RiskDetails(s, i),
        insurers);
  }
}
