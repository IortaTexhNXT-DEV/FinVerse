package com.iortatechnxt.brokerverse.payables.domain;

import com.iortatechnxt.brokerverse.party.domain.PartyType;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * What a payment settles; decides the accounting event (and therefore which payable control account
 * is debited by the configured rule).
 *
 * <table>
 *   <caption>Category, event and eligible parties</caption>
 *   <tr><th>Category</th><th>Event</th><th>Parties</th></tr>
 *   <tr><td>SUPPLIER</td><td>SUPPLIER_PAYMENT</td><td>suppliers, garages, surveyors</td></tr>
 *   <tr><td>COMMISSION</td><td>COMMISSION_PAYMENT</td><td>agents, brokers</td></tr>
 *   <tr><td>CLAIM</td><td>CLAIM_PAYMENT</td><td>claimants, garages, surveyors</td></tr>
 *   <tr><td>REINSURANCE</td><td>RI_SETTLEMENT_PAYMENT</td><td>reinsurers, RI brokers</td></tr>
 *   <tr><td>PREMIUM_REFUND</td><td>PREMIUM_REFUND_PAYMENT</td><td>policyholders</td></tr>
 * </table>
 */
public enum PaymentCategory {
  SUPPLIER(
      "SUPPLIER_PAYMENT", EnumSet.of(PartyType.SUPPLIER, PartyType.GARAGE, PartyType.SURVEYOR)),
  COMMISSION("COMMISSION_PAYMENT", EnumSet.of(PartyType.AGENT, PartyType.BROKER)),
  CLAIM(
      "CLAIM_PAYMENT",
      EnumSet.of(
          PartyType.INDIVIDUAL_CLIENT,
          PartyType.CORPORATE_CLIENT,
          PartyType.GARAGE,
          PartyType.SURVEYOR)),
  REINSURANCE("RI_SETTLEMENT_PAYMENT", EnumSet.of(PartyType.REINSURER, PartyType.RI_BROKER)),
  PREMIUM_REFUND(
      "PREMIUM_REFUND_PAYMENT",
      EnumSet.of(PartyType.INDIVIDUAL_CLIENT, PartyType.CORPORATE_CLIENT));

  /** Open item document type of supplier invoices. */
  public static final String SUPPLIER_INVOICE_DOCUMENT = "SUPPLIER_INVOICE";

  private static final Map<PartyType, PaymentCategory> BY_PARTY_TYPE =
      Map.of(
          PartyType.SUPPLIER, SUPPLIER,
          PartyType.AGENT, COMMISSION,
          PartyType.BROKER, COMMISSION,
          PartyType.REINSURER, REINSURANCE,
          PartyType.RI_BROKER, REINSURANCE,
          PartyType.GARAGE, CLAIM,
          PartyType.SURVEYOR, CLAIM);

  private final String eventType;
  private final Set<PartyType> partyTypes;

  PaymentCategory(String eventType, Set<PartyType> partyTypes) {
    this.eventType = eventType;
    this.partyTypes = partyTypes;
  }

  /**
   * Accounting event published for payments of this category.
   *
   * @return event type code
   */
  public String eventType() {
    return eventType;
  }

  /**
   * Whether a party of the given type may receive a payment of this category.
   *
   * @param type party type
   * @return true when allowed
   */
  public boolean accepts(PartyType type) {
    return partyTypes.contains(type);
  }

  /**
   * Default category for a party type and the document types being paid: supplier invoices are
   * always paid as SUPPLIER; otherwise the party type decides (policyholders default to CLAIM
   * unless every item is a refund / credit note).
   *
   * @param type party type
   * @param documentTypes document types of the selected open items
   * @return category, or null when the party type cannot be paid (coinsurers, banks)
   */
  public static PaymentCategory defaultFor(PartyType type, Set<String> documentTypes) {
    if (!documentTypes.isEmpty()
        && documentTypes.stream().allMatch(SUPPLIER_INVOICE_DOCUMENT::equals)) {
      return SUPPLIER;
    }
    if (PREMIUM_REFUND.accepts(type)) {
      boolean refunds =
          !documentTypes.isEmpty() && documentTypes.stream().noneMatch(d -> d.contains("CLAIM"));
      return refunds ? PREMIUM_REFUND : CLAIM;
    }
    return BY_PARTY_TYPE.get(type);
  }
}
