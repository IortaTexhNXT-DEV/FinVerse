package com.iortatechnxt.brokerverse.nbreport.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.nbreport.domain.SalesTarget;
import com.iortatechnxt.brokerverse.nbreport.domain.SalesTargetRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production targets per sales unit and period (BRNB.075). A target is identified by its unit and
 * period start: saving the same unit and start again changes it. The hierarchy and values are open
 * with BDOI (Q41); every change is audited.
 */
@Service
@Transactional
public class SalesTargetService {

  private static final String ENTITY = "SalesTarget";

  private final SalesTargetRepository targets;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param targets targets
   * @param audit audit trail
   */
  public SalesTargetService(SalesTargetRepository targets, AuditTrailService audit) {
    this.targets = targets;
    this.audit = audit;
  }

  /**
   * Targets whose period overlaps a date range.
   *
   * @param companyId company
   * @param from range start
   * @param to range end
   * @return targets by level, unit and period
   */
  @Transactional(readOnly = true)
  public List<SalesTarget> targets(long companyId, LocalDate from, LocalDate to) {
    return targets.overlapping(companyId, from, to);
  }

  /**
   * Adds or changes the target of a unit for the period starting on the unit's start date.
   *
   * @param companyId company
   * @param unit unit and period
   * @param values target values
   * @return target
   */
  public SalesTarget save(long companyId, SalesTarget.Unit unit, SalesTarget.Values values) {
    var existing =
        targets.findByCompanyIdAndUnitLevelAndUnitCodeAndPeriodFrom(
            companyId, unit.level(), unit.code().strip(), unit.periodFrom());
    SalesTarget target;
    if (existing.isPresent()) {
      target = existing.get();
      target.apply(unit.periodFrom(), unit.periodTo(), values);
    } else {
      target = targets.save(new SalesTarget(companyId, unit, values));
    }
    audit.record(
        ENTITY,
        target.getId(),
        existing.isPresent() ? AuditAction.UPDATE : AuditAction.CREATE,
        "Target "
            + unit.level()
            + " "
            + target.getUnitCode()
            + " "
            + unit.periodFrom()
            + " to "
            + unit.periodTo()
            + ": "
            + values.count()
            + " bookings, premium PHP "
            + target.getTargetPremium()
            + ", commission PHP "
            + target.getTargetCommission());
    return target;
  }
}
