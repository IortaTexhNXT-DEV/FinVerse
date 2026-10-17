package com.iortatechnxt.brokerverse.workflow.api.dto;

import com.iortatechnxt.brokerverse.workflow.service.QueueQuery;

/**
 * Work queue request parameters.
 *
 * @param companyId company
 * @param workflow workflow filter
 * @param stage stage filter
 * @param scope MINE, UNASSIGNED or ALL (default ALL)
 * @param overdue only overdue items
 * @param text reference / title contains
 */
public record QueueParams(
    Long companyId,
    String workflow,
    String stage,
    QueueQuery.Scope scope,
    Boolean overdue,
    String text) {

  /**
   * As a service query.
   *
   * @return query
   */
  public QueueQuery toQuery() {
    return new QueueQuery(
        companyId,
        workflow,
        stage,
        scope == null ? QueueQuery.Scope.ALL : scope,
        Boolean.TRUE.equals(overdue),
        text);
  }
}
