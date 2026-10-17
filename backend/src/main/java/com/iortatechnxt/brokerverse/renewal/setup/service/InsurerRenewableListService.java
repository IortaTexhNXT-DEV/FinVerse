package com.iortatechnxt.brokerverse.renewal.setup.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerRenewableRisk;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerRenewableRiskRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The insurer renewable lists of Renewal Setup (Annex BRRN.020 SC-10; FR-RN-020, 112; CLR-RN-38):
 * per insurer, the risk codes it renews. Every change waits for a checker other than its maker; the
 * check {@code INSURER_RENEWABLE_LIST} reads the authorized rows in force.
 */
@Service
@Transactional
public class InsurerRenewableListService {

  /** Audit entity. */
  public static final String ENTITY = "RenewalInsurerRenewableList";

  private final InsurerRenewableRiskRepository rows;
  private final InsurerService insurers;
  private final ProductCatalogService catalog;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param rows insurer renewable lists
   * @param insurers insurer master
   * @param catalog product catalogue (risk codes)
   * @param audit audit trail
   * @param currentUser signed-in user
   * @param clock clock
   */
  public InsurerRenewableListService(
      InsurerRenewableRiskRepository rows,
      InsurerService insurers,
      ProductCatalogService catalog,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.rows = rows;
    this.insurers = insurers;
    this.catalog = catalog;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The rows of a company.
   *
   * @param companyId company
   * @return rows by insurer and risk code
   */
  @Transactional(readOnly = true)
  public List<InsurerRenewableRisk> list(Long companyId) {
    return rows.findByCompanyIdOrderByInsurerCodeAscRiskCodeAscIdAsc(companyId);
  }

  /**
   * Adds a risk code to the renewable list of an insurer, pending authorization.
   *
   * @param companyId company
   * @param data insurer, risk code, remarks and dates
   * @return row
   */
  public InsurerRenewableRisk create(Long companyId, InsurerRenewableRisk.Data data) {
    InsurerRenewableRisk.Data checked = check(data);
    insurers.requireInsurer(companyId, checked.insurerCode());
    catalog.requireProduct(checked.riskCode());
    InsurerRenewableRisk row = rows.save(new InsurerRenewableRisk(companyId, checked));
    audit.record(ENTITY, row.getId(), AuditAction.CREATE, describe(row));
    return row;
  }

  /**
   * Changes the remarks or dates of a row; it must be authorized again.
   *
   * @param companyId company
   * @param id row
   * @param data new values
   * @return row
   */
  public InsurerRenewableRisk update(Long companyId, Long id, InsurerRenewableRisk.Data data) {
    InsurerRenewableRisk row = row(companyId, id);
    row.update(check(data));
    audit.record(ENTITY, id, AuditAction.UPDATE, describe(row));
    return row;
  }

  /**
   * Authorizes a row (a checker other than its maker).
   *
   * @param companyId company
   * @param id row
   * @return row
   */
  public InsurerRenewableRisk authorize(Long companyId, Long id) {
    InsurerRenewableRisk row = row(companyId, id);
    row.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, id, AuditAction.AUTHORIZE, describe(row));
    return row;
  }

  /**
   * Deactivates a row.
   *
   * @param companyId company
   * @param id row
   * @return row
   */
  public InsurerRenewableRisk deactivate(Long companyId, Long id) {
    InsurerRenewableRisk row = row(companyId, id);
    row.deactivate();
    audit.record(ENTITY, id, AuditAction.DEACTIVATE, describe(row));
    return row;
  }

  private InsurerRenewableRisk row(Long companyId, Long id) {
    return rows.findById(id)
        .filter(r -> r.getCompanyId().equals(companyId))
        .orElseThrow(() -> new ResourceNotFoundException("Insurer renewable list", id));
  }

  private static InsurerRenewableRisk.Data check(InsurerRenewableRisk.Data data) {
    if (blank(data.insurerCode()) || blank(data.riskCode()) || data.effectiveFrom() == null) {
      throw new BusinessRuleException(
          "RNW_RENEWABLE_LIST_INCOMPLETE", "Choose the insurer, the risk code and the start date");
    }
    return new InsurerRenewableRisk.Data(
        data.insurerCode().strip(),
        data.riskCode().strip(),
        blank(data.remarks()) ? null : data.remarks().strip(),
        data.effectiveFrom(),
        data.effectiveTo());
  }

  private static boolean blank(String s) {
    return s == null || s.isBlank();
  }

  private static String describe(InsurerRenewableRisk row) {
    return "Insurer "
        + row.getInsurerCode()
        + " renews risk code "
        + row.getRiskCode()
        + " from "
        + row.getEffectiveFrom();
  }
}
