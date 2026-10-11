package com.iortatechnxt.brokerverse.audit.api;

import com.iortatechnxt.brokerverse.audit.api.dto.AuditLogResponse;
import com.iortatechnxt.brokerverse.audit.api.dto.AuditSearchRequest;
import com.iortatechnxt.brokerverse.audit.service.AuditModules;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailQuery;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only audit trail inquiry (BDOI FRS FRUM.008.02 and FRPM.021.01): filters by date range,
 * user, record type, reference number and action, sortable columns.
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditController {

  /** Record types of the Product Maintenance Audit Logs. */
  static final String PRODUCT_MAINTENANCE = "PRODUCT_MAINTENANCE";

  private final AuditTrailQuery query;

  /**
   * Creates the controller.
   *
   * @param query audit trail search
   */
  public AuditController(AuditTrailQuery query) {
    this.query = query;
  }

  /**
   * Searches the audit trail.
   *
   * @param request filters, sort and page
   * @return page of entries
   */
  @GetMapping
  @PreAuthorize("hasAuthority('AUDIT_VIEW')")
  public PageResponse<AuditLogResponse> search(@Valid AuditSearchRequest request) {
    if (request.to().isBefore(request.from())) {
      throw new BusinessRuleException(
          "DATE_RANGE", "The end date must be on or after the start date");
    }
    AuditTrailQuery.Filter filter =
        new AuditTrailQuery.Filter(
            request.from(),
            request.to(),
            blankToNull(request.username()),
            entityTypes(request.entityType()),
            blankToNull(request.entityId()),
            request.action(),
            request.sort(),
            request.direction());
    return PageResponse.of(
        query.search(filter, request.pageOrFirst(), request.sizeOrDefault()),
        AuditLogResponse::from);
  }

  /**
   * The record types of a filter value.
   *
   * @param entityType record types separated by commas, or PRODUCT_MAINTENANCE
   * @return record types, empty for all
   */
  static List<String> entityTypes(String entityType) {
    String value = blankToNull(entityType);
    if (value == null) {
      return List.of();
    }
    if (PRODUCT_MAINTENANCE.equals(value)) {
      return AuditModules.PRODUCT_MAINTENANCE_TYPES;
    }
    List<String> types = new ArrayList<>();
    Arrays.stream(value.split(",")).map(String::trim).filter(t -> !t.isEmpty()).forEach(types::add);
    return types;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
