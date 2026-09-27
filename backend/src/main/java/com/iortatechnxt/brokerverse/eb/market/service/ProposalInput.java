package com.iortatechnxt.brokerverse.eb.market.service;

import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalFactor;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalItem;
import com.iortatechnxt.brokerverse.eb.domain.EbProposalLine;
import java.time.LocalDate;
import java.util.List;

/**
 * A proposal as the AO enters it from the insurer's e-mail (FR-EB-040).
 *
 * @param insurerCode insurer
 * @param receivedOn date received, not after today; today when null
 * @param validUntil validity, may be null
 * @param currency currency; PHP when null
 * @param terms terms and additional benefits, may be null
 * @param exclusions exclusions, may be null
 * @param lines premium per benefit line and plan, at least one
 * @param items answers to the TOR items (every requested change of a revision)
 * @param factors capability factors
 * @param document the insurer's proposal document, required
 */
public record ProposalInput(
    String insurerCode,
    LocalDate receivedOn,
    LocalDate validUntil,
    String currency,
    String terms,
    String exclusions,
    List<EbProposalLine.Data> lines,
    List<EbProposalItem.Data> items,
    List<EbProposalFactor.Data> factors,
    UploadedFile document) {

  /** Null lists become empty. */
  public ProposalInput {
    lines = lines == null ? List.of() : List.copyOf(lines);
    items = items == null ? List.of() : List.copyOf(items);
    factors = factors == null ? List.of() : List.copyOf(factors);
  }

  /**
   * The same proposal with its document.
   *
   * @param file document
   * @return input
   */
  public ProposalInput withDocument(UploadedFile file) {
    return new ProposalInput(
        insurerCode,
        receivedOn,
        validUntil,
        currency,
        terms,
        exclusions,
        lines,
        items,
        factors,
        file);
  }
}
