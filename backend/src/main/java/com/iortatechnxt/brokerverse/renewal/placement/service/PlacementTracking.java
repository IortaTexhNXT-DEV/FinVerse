package com.iortatechnxt.brokerverse.renewal.placement.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessage;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessageRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CandidatePlacement;
import com.iortatechnxt.brokerverse.renewal.domain.PlacementFile;
import com.iortatechnxt.brokerverse.renewal.domain.PlacementFileRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalPlacement;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalPlacementRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RemarkService;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import com.iortatechnxt.brokerverse.renewal.service.RenewalBatch;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.renewal.service.RenewalWorkingDays;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The placements of a renewal account as the Processing Officer follows them (FRRN.29.05,
 * FRRN.031.01): each insurer's slip with its transmission status, turnaround time and SLA status,
 * the With Issue tag with its resolution date; the cancellation of a rejected placement and the
 * return of an account submitted for placement or rejected to Marketing with one of the return
 * reasons (status Returned - Renewal).
 */
@Service
public class PlacementTracking {

  /** List of the return reasons. */
  public static final String LOV_RETURN = "RNW_PLACEMENT_RETURN_REASON";

  private final RenewalRecords records;
  private final RenewalPlacementRepository placements;
  private final PlacementFileRepository files;
  private final ChannelMessageRepository messages;
  private final PlacementGeneration generation;
  private final RenewalWorkingDays workingDays;
  private final RenewalFlow flow;
  private final RenewalBatch batch;
  private final LovService lovs;
  private final RemarkService remarks;
  private final RenewalNotices notices;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param placements placements
   * @param files placement files
   * @param messages channel messages (transmission status)
   * @param generation cancellation of open placements
   * @param workingDays business days
   * @param flow workflow
   * @param batch one transaction per account
   * @param lovs return reasons
   * @param remarks remarks
   * @param notices notifications
   * @param audit audit trail
   * @param clock clock
   */
  public PlacementTracking(
      RenewalRecords records,
      RenewalPlacementRepository placements,
      PlacementFileRepository files,
      ChannelMessageRepository messages,
      PlacementGeneration generation,
      RenewalWorkingDays workingDays,
      RenewalFlow flow,
      RenewalBatch batch,
      LovService lovs,
      RemarkService remarks,
      RenewalNotices notices,
      AuditTrailService audit,
      Clock clock) {
    this.records = records;
    this.placements = placements;
    this.files = files;
    this.messages = messages;
    this.generation = generation;
    this.workingDays = workingDays;
    this.flow = flow;
    this.batch = batch;
    this.lovs = lovs;
    this.remarks = remarks;
    this.notices = notices;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The placements of a renewal, newest first.
   *
   * @param companyId company
   * @param ref renewal
   * @return placements
   */
  @Transactional(readOnly = true)
  public List<View> of(Long companyId, String ref) {
    RenewalCandidate c = records.get(companyId, ref);
    Predicate<LocalDate> working = workingDays.calendar(companyId);
    LocalDate today = BusinessClock.today(clock);
    return placements.findByCandidateIdOrderByIdDesc(c.getId()).stream()
        .map(p -> view(companyId, p, PlacementTat.measure(p, today, working)))
        .toList();
  }

  /**
   * Tags or clears the With Issue tag of a placement.
   *
   * @param companyId company
   * @param ref renewal
   * @param placementId placement
   * @param withIssue whether the placement has an issue
   * @param resolution resolution date, may be null
   * @return the placement
   */
  @Transactional
  public View issue(
      Long companyId, String ref, Long placementId, boolean withIssue, LocalDate resolution) {
    RenewalCandidate c = records.get(companyId, ref);
    RenewalPlacement p =
        placements
            .findById(placementId)
            .filter(x -> x.getCandidateId().equals(c.getId()))
            .orElseThrow(
                () -> new BusinessRuleException("RNW_PLACEMENT_NOT_FOUND", "Placement not found"));
    requireResolution(p, resolution);
    p.issue(withIssue, resolution);
    audit.record(
        RenewalCodes.ENTITY,
        ref,
        AuditAction.UPDATE,
        "Placement with "
            + p.getInsurerCode()
            + (withIssue ? " tagged With Issue" : " issue cleared")
            + (resolution == null ? "" : ", resolved on " + resolution));
    return view(
        companyId,
        p,
        PlacementTat.measure(p, BusinessClock.today(clock), workingDays.calendar(companyId)));
  }

  private void requireResolution(RenewalPlacement p, LocalDate resolution) {
    if (p.getSubmittedAt() == null) {
      throw new BusinessRuleException(
          "RNW_PLACEMENT_NOT_SENT", "The placement has not been sent to the insurer");
    }
    if (resolution == null) {
      return;
    }
    if (resolution.isBefore(BusinessClock.dateOf(p.getSubmittedAt()))) {
      throw new BusinessRuleException(
          "RNW_PLACEMENT_RESOLUTION",
          "The resolution date cannot be earlier than the placement submission date");
    }
    if (resolution.isAfter(BusinessClock.today(clock))) {
      throw new BusinessRuleException(
          "RNW_PLACEMENT_RESOLUTION", "The resolution date cannot be in the future");
    }
  }

  /**
   * Cancels the placement of a rejected account so it can be placed again.
   *
   * @param companyId company
   * @param ref renewal
   * @param text remarks
   */
  @Transactional
  public void cancel(Long companyId, String ref, String text) {
    RenewalCandidate c = records.get(companyId, ref);
    if (!CandidatePlacement.REJECTED_PLACEMENT.equals(c.getPlacement().getStatus())) {
      throw new BusinessRuleException(
          "RNW_PLACEMENT_CANCEL", "Only a rejected placement can be cancelled");
    }
    String why = RemarkService.requireText(text, "Enter the remarks");
    placements.findByCandidateIdOrderByIdDesc(c.getId()).stream()
        .filter(RenewalPlacement::isCurrent)
        .forEach(RenewalPlacement::cancel);
    c.getPlacement().status(null);
    remarks.add(c, "Placement cancelled: " + why);
    audit.record(RenewalCodes.ENTITY, ref, AuditAction.UPDATE, "Placement cancelled: " + why);
  }

  /**
   * Returns renewal accounts submitted for placement or rejected to Marketing.
   *
   * @param companyId company
   * @param refs renewals
   * @param reasonCode return reason
   * @param text remarks
   * @return returned and refused renewals
   */
  public BatchOutcome returnToMarketing(
      Long companyId, List<String> refs, String reasonCode, String text) {
    if (reasonCode == null || reasonCode.isBlank()) {
      throw new BusinessRuleException("WORKFLOW_REASON_REQUIRED", "Select a reason for the return");
    }
    lovs.requireValid(LOV_RETURN, reasonCode, BusinessClock.today(clock));
    String why = RemarkService.requireText(text, "Enter the remarks");
    String label = lovs.label(LOV_RETURN, reasonCode);
    return batch.run(
        refs,
        ref -> {
          RenewalCandidate c = records.get(companyId, ref);
          RenewalRecords.requireStage(c, RenewalStage.FOR_PLACEMENT_BOOKING);
          String status = c.getPlacement().getStatus();
          if (status != null && !CandidatePlacement.REJECTED_PLACEMENT.equals(status)) {
            throw new BusinessRuleException(
                "RNW_PLACEMENT_RETURN",
                "Only an account Submitted for Placement or Rejected Placement can be returned");
          }
          flow.act(c, "return_placement", new TransitionNote(reasonCode, why));
          generation.cancelOpen(c);
          c.getPlacement().status(null);
          c.getFlags().setReturned(true);
          remarks.add(c, "Returned - Renewal (" + label + "): " + why);
          audit.record(RenewalCodes.ENTITY, ref, AuditAction.REJECT, label + ": " + why);
          notices.users(
              Collections.singletonList(c.getAssignedAo()),
              RenewalCodes.EVENT_RETURNED,
              c,
              new RenewalNotices.Text(ref + " returned by Processing", label + ": " + why));
        });
  }

  private View view(Long companyId, RenewalPlacement p, PlacementTat.Measure tat) {
    PlacementFile file =
        p.getPlacementFileId() == null ? null : files.findById(p.getPlacementFileId()).orElse(null);
    String transmission =
        p.getMessageNo() == null
            ? p.getTransmissionStatus()
            : messages
                .findByCompanyIdAndMessageNo(companyId, p.getMessageNo())
                .map(ChannelMessage::getStatus)
                .map(Enum::name)
                .orElse(p.getTransmissionStatus());
    return new View(
        p.getId(),
        p.getInsurerCode(),
        p.getSharePercent(),
        p.getPremium(),
        p.getSumInsured(),
        new Docs(
            p.getSlipFileName(),
            p.getSlipAttachmentId(),
            file == null ? null : file.getFileName(),
            file == null ? null : file.getAttachmentId()),
        p.getStatus(),
        new Sending(p.getChannel(), p.getMessageNo(), transmission, p.getRecipients(), p.getCc()),
        p.getSubmittedAt() == null ? null : BusinessClock.dateOf(p.getSubmittedAt()),
        tat,
        p.isWithIssue(),
        p.getResolutionDate(),
        new Answer(
            p.getResponse(),
            p.getResponseDate(),
            p.getResponseReason() == null
                ? null
                : lovs.label(PlacementResponses.LOV_REJECT, p.getResponseReason()),
            p.getResponseRemarks()),
        p.getCreatedBy(),
        p.getCreatedAt() == null ? null : BusinessClock.dateOf(p.getCreatedAt()));
  }

  /**
   * A placement.
   *
   * @param id id
   * @param insurerCode insurer
   * @param share share in percent
   * @param premium premium of the share
   * @param sumInsured sum insured of the share
   * @param docs slip and placement file
   * @param status GENERATED, SENT, APPROVED, REJECTED, CANCELLED
   * @param sending how it was sent
   * @param submitted placement submission date
   * @param tat turnaround time, null before sending
   * @param withIssue With Issue tag
   * @param resolutionDate resolution date
   * @param response insurer's response
   * @param generatedBy user who generated the slip
   * @param generatedOn generation date
   */
  public record View(
      Long id,
      String insurerCode,
      BigDecimal share,
      BigDecimal premium,
      BigDecimal sumInsured,
      Docs docs,
      String status,
      Sending sending,
      LocalDate submitted,
      PlacementTat.Measure tat,
      boolean withIssue,
      LocalDate resolutionDate,
      Answer response,
      String generatedBy,
      LocalDate generatedOn) {}

  /**
   * The documents of a placement.
   *
   * @param slipFileName placement slip
   * @param slipAttachmentId stored slip
   * @param fileName consolidated placement file, may be null
   * @param fileAttachmentId stored placement file, may be null
   */
  public record Docs(
      String slipFileName, Long slipAttachmentId, String fileName, Long fileAttachmentId) {}

  /**
   * How a placement was sent.
   *
   * @param channel MFT or CCM
   * @param messageNo channel message
   * @param status transmission status
   * @param recipients recipients or MFT location
   * @param cc copy recipients
   */
  public record Sending(
      String channel, String messageNo, String status, String recipients, String cc) {}

  /**
   * The insurer's response.
   *
   * @param response APPROVED or REJECTED, null while waiting
   * @param date response date
   * @param reason rejection reason
   * @param remarks insurer remarks
   */
  public record Answer(String response, LocalDate date, String reason, String remarks) {}
}
