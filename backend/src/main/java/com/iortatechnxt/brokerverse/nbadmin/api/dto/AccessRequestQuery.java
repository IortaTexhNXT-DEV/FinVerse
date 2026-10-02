package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestSearch;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestSearch.Scope;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Query parameters of the Access Requests work list (FR-UA-017, FR-UA-018).
 *
 * @param scope tab (MINE, ASSIGNED, SECOND, IMPLEMENTATION, ALL)
 * @param status status
 * @param type request type
 * @param text user name, role or request number
 * @param requester requester
 * @param approver approver
 * @param from requested on or after
 * @param to requested on or before
 * @param groupProfiles true for group-profile requests only, false for user requests only
 * @param page page, from 0
 */
public record AccessRequestQuery(
    Scope scope,
    AccessRequestStatus status,
    AccessRequestType type,
    String text,
    String requester,
    String approver,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
    Boolean groupProfiles,
    @Min(0) Integer page) {

  /**
   * The service filter.
   *
   * @return search
   */
  public AccessRequestSearch search() {
    return new AccessRequestSearch(
        scope, status, type, text, requester, approver, from, to, groupProfiles);
  }

  /**
   * The page number.
   *
   * @return page, 0 when absent
   */
  public int pageOrFirst() {
    return page == null ? 0 : page;
  }
}
