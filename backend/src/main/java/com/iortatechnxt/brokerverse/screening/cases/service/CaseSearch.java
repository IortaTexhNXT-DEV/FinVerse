package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseSla.SlaState;
import java.time.LocalDate;

/**
 * The criteria of the case list (SNSRP-402, 403; FR-SS-041, 042): the tab, the search text (case
 * number, client name or code, from 3 characters) and the filters.
 *
 * @param companyId company
 * @param tab MY, TEAM, APPROVAL, COMMITTEE, STR, CLOSED or ALL
 * @param q search text, may be blank
 * @param stage stage filter
 * @param caseType case type filter
 * @param riskCategory risk category filter
 * @param marketingUnit marketing unit filter
 * @param unitHead unit head filter
 * @param disposition disposition filter
 * @param assignee assignee filter
 * @param sla SLA state filter
 * @param createdFrom created on or after
 * @param createdTo created on or before
 */
public record CaseSearch(
    Long companyId,
    Tab tab,
    String q,
    CaseStage stage,
    String caseType,
    String riskCategory,
    String marketingUnit,
    String unitHead,
    String disposition,
    String assignee,
    SlaState sla,
    LocalDate createdFrom,
    LocalDate createdTo) {

  /** The status tabs of the Cases screen. */
  public enum Tab {
    /** Assigned to me. */
    MY,
    /** My team's open cases (all open cases for coordinators). */
    TEAM,
    /** Waiting for the unit head or Compliance. */
    APPROVAL,
    /** Waiting for the AML Committee. */
    COMMITTEE,
    /** STR preparation and extraction. */
    STR,
    /** Closed. */
    CLOSED,
    /** Every case in scope. */
    ALL
  }
}
