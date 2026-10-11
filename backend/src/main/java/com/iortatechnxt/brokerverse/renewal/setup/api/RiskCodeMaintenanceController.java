package com.iortatechnxt.brokerverse.renewal.setup.api;

import com.iortatechnxt.brokerverse.renewal.domain.NonRenewableRiskCode;
import com.iortatechnxt.brokerverse.renewal.setup.service.RiskCodeMaintenance;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Risk Code Maintenance (FRRN.038): read by every Renewal user, maintained by the users of the
 * Renewal setup; authorization by another user goes through the Renewal setup.
 */
@RestController
@RequestMapping("/api/v1/renewal/setup/risk-code-maintenance")
public class RiskCodeMaintenanceController {

  private static final String SETUP = "hasAuthority('RNW_SETUP')";

  private final RiskCodeMaintenance maintenance;

  /**
   * Creates the controller.
   *
   * @param maintenance risk code maintenance
   */
  public RiskCodeMaintenanceController(RiskCodeMaintenance maintenance) {
    this.maintenance = maintenance;
  }

  /**
   * The risk codes.
   *
   * @param companyId company
   * @param search risk code fragment
   * @param renewable indicator filter
   * @return codes
   */
  @GetMapping
  @PreAuthorize("hasAuthority('RNW_VIEW')")
  public List<View> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Boolean renewable) {
    return maintenance.list(companyId, search, renewable).stream().map(View::of).toList();
  }

  /**
   * Adds a risk code.
   *
   * @param companyId company
   * @param entry entry
   * @return code
   */
  @PostMapping
  @PreAuthorize(SETUP)
  public View create(@RequestParam Long companyId, @RequestBody RiskCodeMaintenance.Entry entry) {
    return View.of(maintenance.create(companyId, entry));
  }

  /**
   * Updates a risk code.
   *
   * @param companyId company
   * @param id code
   * @param entry entry
   * @return code
   */
  @PutMapping("/{id}")
  @PreAuthorize(SETUP)
  public View update(
      @RequestParam Long companyId,
      @PathVariable Long id,
      @RequestBody RiskCodeMaintenance.Entry entry) {
    return View.of(maintenance.update(companyId, id, entry));
  }

  /**
   * A risk code.
   *
   * @param id id
   * @param riskCode risk code
   * @param description description
   * @param lineCode product line
   * @param renewable indicator
   * @param effectiveDate effective date
   * @param remarks remarks
   * @param status record status
   * @param createdBy creator
   * @param createdAt creation time
   * @param updatedBy last updater
   * @param updatedAt last update time
   */
  public record View(
      Long id,
      String riskCode,
      String description,
      String lineCode,
      boolean renewable,
      LocalDate effectiveDate,
      String remarks,
      String status,
      String createdBy,
      Instant createdAt,
      String updatedBy,
      Instant updatedAt) {

    static View of(NonRenewableRiskCode c) {
      return new View(
          c.getId(),
          c.getRiskCode(),
          c.getDescription(),
          c.getLineCode(),
          c.isRenewable(),
          c.getEffectiveFrom(),
          c.getRemarks(),
          c.getRecordStatus().name(),
          c.getCreatedBy(),
          c.getCreatedAt(),
          c.getUpdatedBy(),
          c.getUpdatedAt());
    }
  }
}
