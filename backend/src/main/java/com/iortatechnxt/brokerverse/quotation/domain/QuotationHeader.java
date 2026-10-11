package com.iortatechnxt.brokerverse.quotation.domain;

/**
 * Fixed facts of a new quotation.
 *
 * @param companyId company
 * @param quotationNo quotation number (Proposal No.)
 * @param arn Account Reference Number (BRNB.102)
 * @param requestId quotation request it answers, may be null
 * @param client client facts
 * @param productCode risk code
 * @param lineCode product line
 * @param marketSegment market segment
 * @param sourceChannel source channel
 * @param currency currency
 * @param templateVersion intake template version (BRNB.004)
 */
public record QuotationHeader(
    Long companyId,
    String quotationNo,
    String arn,
    Long requestId,
    ClientFacts client,
    String productCode,
    String lineCode,
    String marketSegment,
    String sourceChannel,
    String currency,
    String templateVersion) {

  /**
   * The client of a quotation (a prospect is allowed).
   *
   * @param id crm client id
   * @param code client or prospect code
   * @param name display name
   * @param email e-mail used to send the quotation
   */
  public record ClientFacts(Long id, String code, String name, String email) {}
}
