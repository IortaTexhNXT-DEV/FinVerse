package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.PlacementChannel;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * A panel insurer.
 *
 * @param id id
 * @param companyId company
 * @param partyCode party code
 * @param name name
 * @param shortName short name
 * @param accreditationNo accreditation number
 * @param accreditedUntil accreditation expiry
 * @param placementChannel placement channel
 * @param placementEmails placement mailboxes
 * @param defaultCreditDays default credit days
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 * @param authorizedBy checker
 */
public record InsurerResponse(
    Long id,
    Long companyId,
    String partyCode,
    String name,
    String shortName,
    String accreditationNo,
    LocalDate accreditedUntil,
    PlacementChannel placementChannel,
    List<String> placementEmails,
    int defaultCreditDays,
    RecordStatus recordStatus,
    String maker,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param e entity
   * @return response
   */
  public static InsurerResponse from(InsurerProfile e) {
    return new InsurerResponse(
        e.getId(),
        e.getCompanyId(),
        e.getPartyCode(),
        e.getName(),
        e.getShortName(),
        e.getAccreditationNo(),
        e.getAccreditedUntil(),
        e.getPlacementChannel(),
        e.getPlacementEmailList(),
        e.getDefaultCreditDays(),
        e.getRecordStatus(),
        e.getMaker(),
        e.getAuthorizedBy());
  }
}
