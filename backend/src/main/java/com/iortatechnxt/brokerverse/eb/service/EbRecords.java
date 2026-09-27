package com.iortatechnxt.brokerverse.eb.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds the programme or cycle of a request within its company (a record of another company is "not
 * found") and guards closed cycles. Shared by the EB services of every wave.
 */
@Service
@Transactional(readOnly = true)
public class EbRecords {

  private final EbProgrammeRepository programmes;
  private final EbCycleRepository cycles;

  /**
   * Creates the lookup.
   *
   * @param programmes programmes
   * @param cycles cycles
   */
  public EbRecords(EbProgrammeRepository programmes, EbCycleRepository cycles) {
    this.programmes = programmes;
    this.cycles = cycles;
  }

  /**
   * A programme of a company.
   *
   * @param companyId company
   * @param programmeId programme
   * @return programme
   */
  public EbProgramme programme(Long companyId, Long programmeId) {
    return programmes
        .findByIdAndCompanyId(programmeId, companyId)
        .orElseThrow(() -> new ResourceNotFoundException(EbCodes.ENTITY_PROGRAMME, programmeId));
  }

  /**
   * A programme of any company, for background jobs that run without a company context.
   *
   * @param programmeId programme
   * @return programme
   */
  public EbProgramme programme(Long programmeId) {
    return programmes
        .findById(programmeId)
        .orElseThrow(() -> new ResourceNotFoundException(EbCodes.ENTITY_PROGRAMME, programmeId));
  }

  /**
   * A cycle of a company.
   *
   * @param companyId company
   * @param cycleId cycle
   * @return cycle
   */
  public EbCycle cycle(Long companyId, Long cycleId) {
    return cycles
        .findByIdAndCompanyId(cycleId, companyId)
        .orElseThrow(() -> new ResourceNotFoundException(EbCodes.ENTITY_CYCLE, cycleId));
  }

  /**
   * An open cycle of a company.
   *
   * @param companyId company
   * @param cycleId cycle
   * @return the cycle, refused when closed
   */
  public EbCycle openCycle(Long companyId, Long cycleId) {
    EbCycle cycle = cycle(companyId, cycleId);
    if (!cycle.isOpen()) {
      throw new BusinessRuleException(
          "EB_CYCLE_CLOSED", "Cycle " + cycle.getCycleNo() + " is closed");
    }
    return cycle;
  }

  /**
   * The programme of a cycle.
   *
   * @param cycle cycle
   * @return programme
   */
  public EbProgramme programmeOf(EbCycle cycle) {
    return programme(cycle.getCompanyId(), cycle.getProgrammeId());
  }
}
