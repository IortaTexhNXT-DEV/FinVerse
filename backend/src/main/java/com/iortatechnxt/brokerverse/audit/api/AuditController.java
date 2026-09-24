package com.iortatechnxt.brokerverse.audit.api;

import com.iortatechnxt.brokerverse.audit.api.dto.AuditLogResponse;
import com.iortatechnxt.brokerverse.audit.domain.AuditLogRepository;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only audit trail inquiry. */
@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditController {

  private static final int MAX_PAGE_SIZE = 200;

  private final AuditLogRepository repository;

  /**
   * Creates the controller.
   *
   * @param repository audit repository
   */
  public AuditController(AuditLogRepository repository) {
    this.repository = repository;
  }

  /**
   * Searches the audit trail.
   *
   * @param from from date (inclusive)
   * @param to to date (inclusive)
   * @param username optional user filter
   * @param entityType optional entity type filter
   * @param entityId optional entity id filter
   * @param page page index
   * @param size page size
   * @return page of entries
   */
  @GetMapping
  @PreAuthorize("hasAuthority('AUDIT_VIEW')")
  @Transactional(readOnly = true)
  public PageResponse<AuditLogResponse> search(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String entityType,
      @RequestParam(required = false) String entityId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    var pageable =
        PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by("occurredAt").descending());
    return PageResponse.of(
        repository.search(
            blankToNull(username),
            blankToNull(entityType),
            blankToNull(entityId),
            from.atStartOfDay().toInstant(ZoneOffset.UTC),
            to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),
            pageable),
        AuditLogResponse::from);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
