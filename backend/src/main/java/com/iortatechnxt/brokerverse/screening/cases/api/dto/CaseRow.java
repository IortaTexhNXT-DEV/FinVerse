package com.iortatechnxt.brokerverse.screening.cases.api.dto;

import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseSla;
import java.time.Instant;

/**
 * A row of the case list (SNSRP-402; FR-SS-041 columns).
 *
 * @param id case id
 * @param caseNo case number
 * @param clientId client
 * @param clientCode client code
 * @param clientName client name
 * @param caseType case type
 * @param riskCategory risk category
 * @param stage stage
 * @param status OPEN or CLOSED
 * @param assignee assignee
 * @param marketingUnit marketing unit
 * @param unitHead unit head
 * @param disposition last disposition
 * @param activePolicy active-policy flag
 * @param createdAt created
 * @param dueAt due time of the stage
 * @param slaState NONE, ON_TIME, DUE_SOON or BREACHED
 */
public record CaseRow(
    Long id,
    String caseNo,
    Long clientId,
    String clientCode,
    String clientName,
    String caseType,
    String riskCategory,
    String stage,
    String status,
    String assignee,
    String marketingUnit,
    String unitHead,
    String disposition,
    boolean activePolicy,
    Instant createdAt,
    Instant dueAt,
    String slaState) {

  /**
   * Maps a case.
   *
   * @param c the case
   * @param now the time of the SLA state
   * @return the row
   */
  public static CaseRow from(ScreeningCase c, Instant now) {
    return new CaseRow(
        c.getId(),
        c.getCaseNo(),
        c.getClientId(),
        c.getClientCode(),
        c.getClientName(),
        c.getCaseType(),
        c.getRiskCategory(),
        c.getStage().name(),
        c.getStatus().name(),
        c.getAssignee(),
        c.getMarketingUnit(),
        c.getUnitHead(),
        c.getDisposition(),
        c.isActivePolicy(),
        c.getCreatedAt(),
        c.getDueAt(),
        CaseSla.state(c, now).name());
  }
}
