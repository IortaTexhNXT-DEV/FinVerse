package com.iortatechnxt.brokerverse.consolidation.api.dto;

import com.iortatechnxt.brokerverse.consolidation.domain.IntercompanyRelationship;

/**
 * Inter-company relationship view.
 *
 * @param id id
 * @param companyAId first company
 * @param aDueFromAccount first company's due-from account
 * @param aDueToAccount first company's due-to account
 * @param companyBId second company
 * @param bDueFromAccount second company's due-from account
 * @param bDueToAccount second company's due-to account
 * @param active active flag
 */
public record RelationshipResponse(
    Long id,
    Long companyAId,
    String aDueFromAccount,
    String aDueToAccount,
    Long companyBId,
    String bDueFromAccount,
    String bDueToAccount,
    boolean active) {

  /**
   * Maps an entity.
   *
   * @param r relationship
   * @return view
   */
  public static RelationshipResponse from(IntercompanyRelationship r) {
    return new RelationshipResponse(
        r.getId(),
        r.getCompanyAId(),
        r.getADueFromAccount(),
        r.getADueToAccount(),
        r.getCompanyBId(),
        r.getBDueFromAccount(),
        r.getBDueToAccount(),
        r.isActive());
  }
}
