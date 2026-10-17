package com.iortatechnxt.brokerverse.eb.renewal.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbRenewalAdvice;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Send RA from the Programmes work list (FR-EB-022, alternate flow "Manual send"): sends the
 * renewal advice of each selected programme in its own transaction and reports, per programme, the
 * cycle of the advice or why it was not sent.
 */
@Component
public class RenewalAdviceBatch {

  /** Most programmes in one Send RA. */
  static final int MAX_PROGRAMMES = 100;

  private final RenewalAdviceService service;
  private final EbRecords records;
  private final Clock clock;

  /**
   * Creates the batch.
   *
   * @param service sending
   * @param records programme and cycle look-up
   * @param clock clock
   */
  public RenewalAdviceBatch(RenewalAdviceService service, EbRecords records, Clock clock) {
    this.service = service;
    this.records = records;
    this.clock = clock;
  }

  /**
   * Sends the renewal advice of several programmes.
   *
   * @param companyId company
   * @param programmeIds programmes
   * @return one result per programme, in the order given
   */
  public List<Result> send(Long companyId, List<Long> programmeIds) {
    if (programmeIds == null || programmeIds.isEmpty() || programmeIds.size() > MAX_PROGRAMMES) {
      throw new BusinessRuleException(
          "EB_RA_SELECTION", "Select between 1 and " + MAX_PROGRAMMES + " programmes");
    }
    LocalDate today = BusinessClock.today(clock);
    List<Result> results = new ArrayList<>();
    for (Long id : new LinkedHashSet<>(programmeIds)) {
      results.add(sendOne(companyId, id, today));
    }
    return results;
  }

  private Result sendOne(Long companyId, Long programmeId, LocalDate today) {
    String number;
    try {
      number = records.programme(companyId, programmeId).getProgrammeNo();
    } catch (ResourceNotFoundException e) {
      return new Result(programmeId, null, false, null, "Programme not found");
    }
    try {
      EbRenewalAdvice advice = service.sendManual(companyId, programmeId, today);
      EbCycle cycle = records.cycle(companyId, advice.getCycleId());
      return new Result(programmeId, number, true, cycle.getCycleNo(), null);
    } catch (BusinessRuleException e) {
      return new Result(programmeId, number, false, null, e.getMessage());
    }
  }

  /**
   * The outcome for one programme.
   *
   * @param programmeId programme
   * @param programmeNo programme number
   * @param sent whether the advice was sent
   * @param cycleNo cycle of the advice
   * @param message why not
   */
  public record Result(
      Long programmeId, String programmeNo, boolean sent, String cycleNo, String message) {}
}
