package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRule;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRule.PendingAction;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestDescriber;
import java.time.Instant;

/**
 * Separation-of-duties rule.
 *
 * @param id identifier
 * @param ruleCode rule number
 * @param profileA first group profile
 * @param profileAName its name
 * @param profileB second group profile
 * @param profileBName its name
 * @param description why one user may not hold both
 * @param status record status
 * @param pendingAction creation or deactivation waiting for an authorisation
 * @param maker user who created the rule or asked for the change
 * @param authorizedBy authorising user
 * @param authorizedAt authorisation time
 * @param createdAt creation time
 */
public record SodRuleResponse(
    Long id,
    String ruleCode,
    String profileA,
    String profileAName,
    String profileB,
    String profileBName,
    String description,
    RecordStatus status,
    PendingAction pendingAction,
    String maker,
    String authorizedBy,
    Instant authorizedAt,
    Instant createdAt) {

  /**
   * Maps a rule.
   *
   * @param r rule
   * @param describer profile names
   * @return response
   */
  public static SodRuleResponse from(SodRule r, AccessRequestDescriber describer) {
    return new SodRuleResponse(
        r.getId(),
        r.getRuleCode(),
        r.getProfileA(),
        describer.profileName(r.getProfileA()),
        r.getProfileB(),
        describer.profileName(r.getProfileB()),
        r.getDescription(),
        r.getRecordStatus(),
        r.getPendingAction(),
        r.getMaker(),
        r.getAuthorizedBy(),
        r.getAuthorizedAt(),
        r.getCreatedAt());
  }
}
