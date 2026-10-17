package com.iortatechnxt.brokerverse.productmaint.api;

import com.iortatechnxt.brokerverse.audit.api.dto.AuditLogResponse;
import com.iortatechnxt.brokerverse.audit.api.dto.AuditSearchRequest;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailQuery;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.productmaint.service.PmAuditScope;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Audit Logs of Product Maintenance (BDOI FRS FRPM.021.01) and the audit logs of one record
 * (FRPM.021.02): the Product Maintenance records only, filtered by date range, action type,
 * reference number and user, with sortable columns.
 */
@RestController
@RequestMapping("/api/v1/product-maintenance/audit-logs")
public class PmAuditController {

  private final AuditTrailQuery query;

  /**
   * Creates the controller.
   *
   * @param query audit trail search
   */
  public PmAuditController(AuditTrailQuery query) {
    this.query = query;
  }

  /**
   * Searches the Product Maintenance audit logs.
   *
   * @param request filters, sort and page (the record type limits the search further)
   * @return page of entries
   */
  @GetMapping
  @PreAuthorize("hasAnyAuthority('PRODUCT_VIEW', 'PKG_REPORT_VIEW', 'AUDIT_VIEW')")
  public PageResponse<AuditLogResponse> search(@Valid AuditSearchRequest request) {
    if (request.to().isBefore(request.from())) {
      throw new BusinessRuleException(
          "DATE_RANGE", "The end date must be on or after the start date");
    }
    String reference = blankToNull(request.entityId());
    AuditTrailQuery.Filter filter =
        new AuditTrailQuery.Filter(
            request.from(),
            request.to(),
            blankToNull(request.username()),
            PmAuditScope.types(request.entityType(), reference),
            reference,
            request.action(),
            request.sort(),
            request.direction());
    return PageResponse.of(
        query.search(filter, request.pageOrFirst(), request.sizeOrDefault()),
        AuditLogResponse::from);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
