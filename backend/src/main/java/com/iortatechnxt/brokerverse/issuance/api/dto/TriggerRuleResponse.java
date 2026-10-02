package com.iortatechnxt.brokerverse.issuance.api.dto;

import com.iortatechnxt.brokerverse.issuance.domain.DocumentTrigger;

/**
 * A document trigger rule (BRNB.105).
 *
 * @param documentType document type
 * @param action action started
 * @param description description
 * @param active in force
 */
public record TriggerRuleResponse(
    String documentType, String action, String description, boolean active) {

  /**
   * Maps a rule.
   *
   * @param t rule
   * @return response
   */
  public static TriggerRuleResponse from(DocumentTrigger t) {
    return new TriggerRuleResponse(
        t.getDocumentType(), t.getAction().name(), t.getDescription(), t.isActive());
  }
}
