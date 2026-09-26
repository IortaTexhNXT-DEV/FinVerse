package com.iortatechnxt.brokerverse.screening.cases.api.dto;

import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseSearch;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseSla.SlaState;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Query parameters of the Cases screen (SNSRP-402, 403; FR-SS-041, 042); they stay in the address
 * of the screen so a filtered list can be shared.
 *
 * @param companyId company
 * @param tab status tab (default ALL)
 * @param q case number, client name or code (from 3 characters)
 * @param stage stage
 * @param caseType case type
 * @param riskCategory risk category
 * @param marketingUnit marketing unit
 * @param unitHead unit head
 * @param disposition disposition
 * @param assignee assignee
 * @param sla SLA state
 * @param createdFrom created on or after
 * @param createdTo created on or before
 * @param page page (default 0)
 * @param size page size (default 25)
 */
public record CaseQuery(
    @NotNull Long companyId,
    CaseSearch.Tab tab,
    String q,
    CaseStage stage,
    String caseType,
    String riskCategory,
    String marketingUnit,
    String unitHead,
    String disposition,
    String assignee,
    SlaState sla,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
    Integer page,
    Integer size) {

  private static final int DEFAULT_SIZE = 25;

  /**
   * The service criteria.
   *
   * @return search
   */
  public CaseSearch search() {
    return new CaseSearch(
        companyId,
        tab == null ? CaseSearch.Tab.ALL : tab,
        q,
        stage,
        caseType,
        riskCategory,
        marketingUnit,
        unitHead,
        disposition,
        assignee,
        sla,
        createdFrom,
        createdTo);
  }

  /**
   * The page number.
   *
   * @return page, 0 by default
   */
  public int pageNumber() {
    return page == null ? 0 : Math.max(0, page);
  }

  /**
   * The page size, capped.
   *
   * @param max the largest size
   * @return size, 25 by default
   */
  public int pageSize(int max) {
    return size == null ? DEFAULT_SIZE : Math.min(Math.max(1, size), max);
  }
}
