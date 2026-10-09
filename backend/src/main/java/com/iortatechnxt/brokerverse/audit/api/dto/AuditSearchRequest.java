package com.iortatechnxt.brokerverse.audit.api.dto;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Filters of the Audit Trail (request parameters).
 *
 * @param from from date (inclusive)
 * @param to to date (inclusive)
 * @param username user, all when blank
 * @param entityType record type; several separated by commas, or PRODUCT_MAINTENANCE for the record
 *     types of Product Maintenance
 * @param entityId part of the reference number
 * @param action action
 * @param sort column to sort by (occurredAt, username, action, entityType, entityId)
 * @param direction asc or desc (default desc)
 * @param page page index (default 0)
 * @param size page size (default 50)
 */
public record AuditSearchRequest(
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
    String username,
    String entityType,
    String entityId,
    AuditAction action,
    String sort,
    String direction,
    Integer page,
    Integer size) {

  private static final int DEFAULT_SIZE = 50;

  /**
   * Page index.
   *
   * @return page, 0 when not given
   */
  public int pageOrFirst() {
    return page == null || page < 0 ? 0 : page;
  }

  /**
   * Page size.
   *
   * @return size, 50 when not given
   */
  public int sizeOrDefault() {
    return size == null || size < 1 ? DEFAULT_SIZE : size;
  }
}
