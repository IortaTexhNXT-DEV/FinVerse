package com.iortatechnxt.brokerverse.renewal.acceptance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalFollowup;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalFollowupRepository;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RemarkService;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contact Center follow-ups (FR-RN-085): channel, outcome, remarks and next action date of each
 * contact with the client, logged with the user and time and shown on the renewal record.
 */
@Service
@Transactional
public class FollowupService {

  private final RenewalRecords records;
  private final RenewalFollowupRepository followups;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param followups follow-ups
   * @param lovs lists of values
   * @param audit audit trail
   * @param clock clock
   */
  public FollowupService(
      RenewalRecords records,
      RenewalFollowupRepository followups,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.records = records;
    this.followups = followups;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Records a follow-up.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @param input follow-up
   * @return follow-up
   */
  public RenewalFollowup record(Long companyId, String renewalRef, Input input) {
    RenewalCandidate c = records.get(companyId, renewalRef);
    LocalDate today = BusinessClock.today(clock);
    if (input.channel() == null || input.outcome() == null) {
      throw new BusinessRuleException(
          "RNW_FOLLOWUP_REQUIRED", "Select the channel and the outcome");
    }
    lovs.requireValid(RenewalCodes.LOV_FOLLOWUP_CHANNEL, input.channel(), today);
    lovs.requireValid(RenewalCodes.LOV_FOLLOWUP_OUTCOME, input.outcome(), today);
    String text = RemarkService.requireText(input.remarks(), "Enter the remarks");
    if (input.nextActionDate() != null && input.nextActionDate().isBefore(today)) {
      throw new BusinessRuleException(
          "RNW_FOLLOWUP_DATE", "The next action date cannot be in the past");
    }
    RenewalFollowup f =
        followups.save(
            new RenewalFollowup(
                c.getId(), input.channel(), input.outcome(), text, input.nextActionDate()));
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.CREATE,
        "Follow-up "
            + lovs.label(RenewalCodes.LOV_FOLLOWUP_CHANNEL, input.channel())
            + ": "
            + lovs.label(RenewalCodes.LOV_FOLLOWUP_OUTCOME, input.outcome()));
    return f;
  }

  /**
   * The follow-ups of a renewal, newest first.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @return follow-ups
   */
  @Transactional(readOnly = true)
  public List<RenewalFollowup> of(Long companyId, String renewalRef) {
    return followups.findByCandidateIdOrderByIdDesc(records.get(companyId, renewalRef).getId());
  }

  /**
   * A follow-up.
   *
   * @param channel channel (list RNW_FOLLOWUP_CHANNEL)
   * @param outcome outcome (list RNW_FOLLOWUP_OUTCOME)
   * @param remarks remarks
   * @param nextActionDate next action date, may be null
   */
  public record Input(String channel, String outcome, String remarks, LocalDate nextActionDate) {}
}
