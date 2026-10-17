package com.iortatechnxt.brokerverse.audit.api;

import com.iortatechnxt.brokerverse.audit.service.MasterChangeLog;
import com.iortatechnxt.brokerverse.audit.service.MasterChangeLog.Entry;
import com.iortatechnxt.brokerverse.audit.service.MasterChangeLogSearch;
import com.iortatechnxt.brokerverse.audit.service.MasterChangeLogSearch.Filter;
import com.iortatechnxt.brokerverse.audit.service.MasterChangeLogWorkbook;
import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.security.CompanyScoped;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import jakarta.validation.constraints.Pattern;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Field-level change history of a master record (G05): old and new value, maker, checker and their
 * times, read-only, for the master data viewers, the client viewers and the auditors. The history
 * of a record owned by a company or branch outside the caller's data scope is answered as not
 * found, so its existence is not disclosed.
 */
@RestController
@Validated
@RequestMapping("/api/v1/change-history")
public class ChangeHistoryController {

  /** Readers of the change history. */
  static final String READ =
      "hasAnyAuthority('AUDIT_VIEW', 'MASTER_VIEW', 'PRODUCT_VIEW', 'CLIENT_VIEW')";

  /** Readers of the programme-wide change log. */
  static final String LOG_READ = "hasAnyAuthority('AUDIT_VIEW', 'MASTER_VIEW')";

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final MasterChangeLog log;
  private final MasterChangeLogSearch search;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param log change log
   * @param search programme-wide change log
   * @param clock clock
   */
  public ChangeHistoryController(MasterChangeLog log, MasterChangeLogSearch search, Clock clock) {
    this.log = log;
    this.search = search;
    this.clock = clock;
  }

  /**
   * The programme-wide Master Data Change Log, within the reader's data scope.
   *
   * @param entityType record type
   * @param from first day
   * @param to last day
   * @param maker maker
   * @param checker checker
   * @return changes, latest first
   */
  @GetMapping("/log")
  @PreAuthorize(LOG_READ)
  @CompanyScoped("MasterChangeLogSearch.search keeps the records of the reader's data scope")
  public List<Entry> changeLog(
      @RequestParam(required = false) @Pattern(regexp = "[A-Za-z]{0,80}") String entityType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) @Pattern(regexp = "[\\w.@-]{0,50}") String maker,
      @RequestParam(required = false) @Pattern(regexp = "[\\w.@-]{0,50}") String checker) {
    return search.search(new Filter(entityType, from, to, maker, checker));
  }

  /**
   * The record types present in the change log.
   *
   * @return types
   */
  @GetMapping("/log/types")
  @PreAuthorize(LOG_READ)
  public List<String> changeLogTypes() {
    return search.entityTypes();
  }

  /**
   * The Master Data Change Log as an Excel file, with the same filters and data scope.
   *
   * @param entityType record type
   * @param from first day
   * @param to last day
   * @param maker maker
   * @param checker checker
   * @return XLSX file
   */
  @GetMapping("/log/export")
  @PreAuthorize(LOG_READ)
  @CompanyScoped("MasterChangeLogSearch.search keeps the records of the reader's data scope")
  public ResponseEntity<byte[]> exportChangeLog(
      @RequestParam(required = false) @Pattern(regexp = "[A-Za-z]{0,80}") String entityType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) @Pattern(regexp = "[\\w.@-]{0,50}") String maker,
      @RequestParam(required = false) @Pattern(regexp = "[\\w.@-]{0,50}") String checker) {
    Filter f = new Filter(entityType, from, to, maker, checker);
    String name = "Master_Data_Change_Log_" + BusinessClock.today(clock) + ".xlsx";
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(XLSX))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(name))
        .body(MasterChangeLogWorkbook.of(f.describe(), search.search(f)));
  }

  /**
   * The change history of a record, latest first.
   *
   * @param entityType record type, e.g. InsurerProfile, RiskProduct, Client
   * @param entityId record id
   * @return field changes
   */
  @GetMapping
  @PreAuthorize(READ)
  @CompanyScoped("MasterChangeLog.scopedHistory resolves the owner of the record")
  public List<Entry> history(
      @RequestParam @Pattern(regexp = "[A-Za-z]{1,80}") String entityType,
      @RequestParam Long entityId) {
    return log.scopedHistory(entityType, entityId);
  }
}
