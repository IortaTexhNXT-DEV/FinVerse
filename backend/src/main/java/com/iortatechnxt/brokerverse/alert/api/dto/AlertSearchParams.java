package com.iortatechnxt.brokerverse.alert.api.dto;

import com.iortatechnxt.brokerverse.alert.domain.AlertSeverity;
import com.iortatechnxt.brokerverse.alert.domain.AlertStatus;
import com.iortatechnxt.brokerverse.alert.service.AlertService.AlertSearch;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Alert inbox filters (all optional).
 *
 * @param status status
 * @param severity severity
 * @param code exception code
 * @param companyId company
 * @param from raised on or after
 * @param to raised on or before
 */
public record AlertSearchParams(
    AlertStatus status,
    AlertSeverity severity,
    String code,
    Long companyId,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

  private static final Instant OPEN_END = Instant.parse("9999-12-31T00:00:00Z");

  /**
   * Converts to service criteria (open date bounds when omitted).
   *
   * @return criteria
   */
  public AlertSearch toCriteria() {
    return new AlertSearch(
        status,
        severity,
        code == null || code.isBlank() ? null : code.strip(),
        companyId,
        from == null ? Instant.EPOCH : from.atStartOfDay().toInstant(ZoneOffset.UTC),
        to == null ? OPEN_END : to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
  }
}
