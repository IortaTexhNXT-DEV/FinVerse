package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconSchedule;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconSchedule.Terms;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconScheduleRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extraction schedules per insurer (PRCID.001): maintained by the reconciliation handlers (the
 * frequency per insurer is parked, OQ29); the next run date rolls to the next working day of the
 * head office calendar (weekly holidays and declared holidays).
 */
@Service
@Transactional
public class ReconScheduleService {

  private static final String ENTITY = "ReconSchedule";
  private static final int MAX_ROLL = 31;

  private final ReconScheduleRepository schedules;
  private final OrganizationService organization;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param schedules schedules
   * @param organization branches and holiday calendar
   * @param audit audit trail
   * @param clock clock
   */
  public ReconScheduleService(
      ReconScheduleRepository schedules,
      OrganizationService organization,
      AuditTrailService audit,
      Clock clock) {
    this.schedules = schedules;
    this.organization = organization;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Schedules of a company.
   *
   * @param companyId company
   * @return schedules by insurer
   */
  @Transactional(readOnly = true)
  public List<ReconSchedule> list(Long companyId) {
    return schedules.findByCompanyIdOrderByInsurerCodeAsc(companyId);
  }

  /**
   * Adds the schedule of an insurer.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param terms terms
   * @return schedule
   */
  public ReconSchedule create(Long companyId, String insurerCode, Terms terms) {
    String insurer = insurerCode.strip();
    if (schedules.findByCompanyIdAndInsurerCode(companyId, insurer).isPresent()) {
      throw new DuplicateResourceException("Extraction schedule", insurer);
    }
    ReconSchedule schedule =
        schedules.save(new ReconSchedule(companyId, insurer, terms, LocalDate.now(clock)));
    roll(schedule);
    audit.record(ENTITY, schedule.getId(), AuditAction.CREATE, describe(schedule));
    return schedule;
  }

  /**
   * Changes a schedule.
   *
   * @param id schedule
   * @param terms terms
   * @return schedule
   */
  public ReconSchedule update(Long id, Terms terms) {
    ReconSchedule schedule =
        schedules.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    schedule.update(terms, LocalDate.now(clock));
    roll(schedule);
    audit.record(ENTITY, id, AuditAction.UPDATE, describe(schedule));
    return schedule;
  }

  /**
   * Rolls a schedule's next run date to a working day (PRCID.001 holiday roll).
   *
   * @param schedule schedule
   */
  void roll(ReconSchedule schedule) {
    Optional<Branch> office =
        organization.listBranches(schedule.getCompanyId()).stream()
            .filter(Branch::isHeadOffice)
            .findFirst();
    if (office.isEmpty()) {
      return;
    }
    LocalDate date = schedule.getNextRunDate();
    for (int i = 0; i < MAX_ROLL && !organization.isWorkingDay(office.get(), date); i++) {
      date = date.plusDays(1);
    }
    schedule.rollTo(date);
  }

  private static String describe(ReconSchedule s) {
    return s.getInsurerCode()
        + " "
        + s.getFrequency()
        + " day "
        + s.getRunDay()
        + ", next "
        + s.getNextRunDate()
        + (s.isActive() ? "" : " (inactive)");
  }
}
