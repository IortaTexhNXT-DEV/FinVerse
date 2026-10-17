package com.iortatechnxt.brokerverse.nonpackage.service;

import com.iortatechnxt.brokerverse.nonpackage.domain.RiskDetails;
import java.time.LocalDate;
import java.util.List;

/**
 * A PRF as entered by Marketing (or updated by TSU, BRNB.007).
 *
 * @param clientId client (a prospect is allowed until the accounts are created)
 * @param productCode risk code
 * @param marketSegment market segment (list MARKET_SEGMENT)
 * @param sourceChannel source channel (list SOURCE_CHANNEL)
 * @param currency currency; PHP when empty
 * @param periodFrom requested period start
 * @param periodTo requested period end
 * @param details free-form sections and risk items
 * @param insurers insurers requested (party codes)
 */
public record ProposalDraft(
    Long clientId,
    String productCode,
    String marketSegment,
    String sourceChannel,
    String currency,
    LocalDate periodFrom,
    LocalDate periodTo,
    RiskDetails details,
    List<String> insurers) {

  /** Defensive copies. */
  public ProposalDraft {
    details = details == null ? RiskDetails.EMPTY : details;
    insurers = insurers == null ? List.of() : List.copyOf(insurers);
  }
}
