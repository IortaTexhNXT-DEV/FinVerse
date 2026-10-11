package com.iortatechnxt.brokerverse.eb.confirmation.service;

import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import java.time.LocalDate;
import java.util.List;

/**
 * The client's confirmation as the AO records it (FR-EB-046).
 *
 * @param channel EMAIL or SIGNED_DOCUMENT
 * @param confirmedOn date of the confirmation, not after today
 * @param remarks remarks, may be null
 * @param choices chosen proposal of each active programme line
 * @param evidence the client's e-mail or signed document, required
 */
public record ConfirmationInput(
    String channel,
    LocalDate confirmedOn,
    String remarks,
    List<Choice> choices,
    UploadedFile evidence) {

  /** Null list becomes empty. */
  public ConfirmationInput {
    choices = choices == null ? List.of() : List.copyOf(choices);
  }

  /**
   * The same input with its evidence.
   *
   * @param file evidence
   * @return input
   */
  public ConfirmationInput withEvidence(UploadedFile file) {
    return new ConfirmationInput(channel, confirmedOn, remarks, choices, file);
  }

  /**
   * The chosen proposal of a programme line.
   *
   * @param lineNo programme line
   * @param proposalId validated proposal offering the line's benefit line
   */
  public record Choice(int lineNo, Long proposalId) {}
}
