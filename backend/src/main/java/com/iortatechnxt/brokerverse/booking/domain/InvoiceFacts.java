package com.iortatechnxt.brokerverse.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Who and what a booked invoice is about, copied from the account at booking: client, lead insurer,
 * risk, segment and the sales stamp with the cost center (BRNB.108).
 *
 * @param clientId client id
 * @param clientCode client code (sub-ledger party of the premium receivable)
 * @param clientName client name
 * @param insurerCode lead insurer party code
 * @param riskCode BDOI risk code (product)
 * @param lineCode product line
 * @param marketSegment market segment
 * @param sourceChannel source channel
 * @param accountOfficer account officer (user name)
 * @param salesUnit sales team
 * @param department sales department
 * @param costCenter cost center (dimension COST_CENTER, mandatory)
 * @param productVersionNo package version of the account (BRPM.007), null when none
 */
@Embeddable
public record InvoiceFacts(
    @Column(name = "client_id", nullable = false) Long clientId,
    @Column(name = "client_code", nullable = false, length = 30) String clientCode,
    @Column(name = "client_name", nullable = false, length = 250) String clientName,
    @Column(name = "insurer_code", nullable = false, length = 30) String insurerCode,
    @Column(name = "risk_code", nullable = false, length = 20) String riskCode,
    @Column(name = "line_code", nullable = false, length = 30) String lineCode,
    @Column(name = "market_segment", length = 40) String marketSegment,
    @Column(name = "source_channel", length = 40) String sourceChannel,
    @Column(name = "account_officer", length = 50) String accountOfficer,
    @Column(name = "sales_unit", length = 20) String salesUnit,
    @Column(name = "department", length = 20) String department,
    @Column(name = "cost_center", nullable = false, length = 20) String costCenter,
    @Column(name = "product_version_no") Integer productVersionNo) {

  /**
   * Facts of a product without package version.
   *
   * @param clientId client id
   * @param clientCode client code
   * @param clientName client name
   * @param insurerCode lead insurer party code
   * @param riskCode risk code
   * @param lineCode product line
   * @param marketSegment market segment
   * @param sourceChannel source channel
   * @param accountOfficer account officer
   * @param salesUnit sales team
   * @param department sales department
   * @param costCenter cost center
   */
  public InvoiceFacts(
      Long clientId,
      String clientCode,
      String clientName,
      String insurerCode,
      String riskCode,
      String lineCode,
      String marketSegment,
      String sourceChannel,
      String accountOfficer,
      String salesUnit,
      String department,
      String costCenter) {
    this(
        clientId,
        clientCode,
        clientName,
        insurerCode,
        riskCode,
        lineCode,
        marketSegment,
        sourceChannel,
        accountOfficer,
        salesUnit,
        department,
        costCenter,
        null);
  }
}
