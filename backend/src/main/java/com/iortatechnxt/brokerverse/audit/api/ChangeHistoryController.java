package com.iortatechnxt.brokerverse.audit.api;

import com.iortatechnxt.brokerverse.audit.service.MasterChangeLog;
import com.iortatechnxt.brokerverse.audit.service.MasterChangeLog.Entry;
import com.iortatechnxt.brokerverse.common.security.CompanyScoped;
import jakarta.validation.constraints.Pattern;
import java.util.List;
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

  private final MasterChangeLog log;

  /**
   * Creates the controller.
   *
   * @param log change log
   */
  public ChangeHistoryController(MasterChangeLog log) {
    this.log = log;
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
