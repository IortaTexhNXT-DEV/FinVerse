package com.iortatechnxt.brokerverse.reinsurance.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import com.iortatechnxt.brokerverse.reinsurance.api.dto.FacAssignRequest;
import com.iortatechnxt.brokerverse.reinsurance.domain.Cession;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacParticipant;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacPlacement;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacPlacementRepository;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Facultative placements: the reinsurance officer records the reinsurers on the slip and submits
 * it; a checker approves it, which cedes the placed premium to each participant ({@code
 * RI_PREMIUM_CEDED}, open item due to the reinsurer); the placement is then closed.
 */
@Service
@Transactional
public class FacPlacementService {

  static final String ENTITY = "FacPlacement";

  private final FacPlacementRepository placements;
  private final PartyService parties;
  private final ReinsuranceAccounting accounting;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param placements repository
   * @param parties reinsurers
   * @param accounting reinsurance postings
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public FacPlacementService(
      FacPlacementRepository placements,
      PartyService parties,
      ReinsuranceAccounting accounting,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.placements = placements;
    this.parties = parties;
    this.accounting = accounting;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists placements.
   *
   * @param companyId company
   * @param status status filter, null for all
   * @return placements, newest first
   */
  @Transactional(readOnly = true)
  public List<FacPlacement> list(Long companyId, FacStatus status) {
    return status == null
        ? placements.findByCompanyIdOrderByIdDesc(companyId)
        : placements.findByCompanyIdAndStatusInOrderByIdDesc(companyId, EnumSet.of(status));
  }

  /**
   * Gets a placement.
   *
   * @param id id
   * @return placement
   */
  @Transactional(readOnly = true)
  public FacPlacement get(Long id) {
    return placements.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Records the reinsurers of a provisional slip.
   *
   * @param id placement
   * @param r participants
   * @return placement
   */
  public FacPlacement assign(Long id, FacAssignRequest r) {
    FacPlacement placement = get(id);
    List<FacParticipant> lines = new ArrayList<>();
    int lineNo = 1;
    for (FacAssignRequest.Line l : r.participants()) {
      lines.add(
          new FacParticipant(
              lineNo++,
              parties.requireActive(
                  placement.getCompanyId(), l.reinsurerCode(), EnumSet.of(PartyType.REINSURER)),
              l.sharePct(),
              l.commissionPct()));
    }
    placement.assign(lines, r.remarks());
    audit.record(ENTITY, placement.getPlacementNo(), AuditAction.UPDATE, "Recorded participants");
    return placement;
  }

  /**
   * Submits a slip for approval.
   *
   * @param id placement
   * @return placement
   */
  public FacPlacement submit(Long id) {
    FacPlacement placement = get(id);
    placement.submit(currentUser.username(), clock.instant());
    audit.record(ENTITY, placement.getPlacementNo(), AuditAction.SUBMIT, "Submitted slip");
    return placement;
  }

  /**
   * Returns a submitted slip to the maker.
   *
   * @param id placement
   * @return placement
   */
  public FacPlacement reject(Long id) {
    FacPlacement placement = get(id);
    placement.reject(currentUser.username());
    audit.record(ENTITY, placement.getPlacementNo(), AuditAction.REJECT, "Returned slip");
    return placement;
  }

  /**
   * Approves a slip (checker): fixes the placed shares and cedes the premium to the participants.
   *
   * @param id placement
   * @param date placement (accounting) date, null for today
   * @return placement
   */
  public FacPlacement approve(Long id, LocalDate date) {
    FacPlacement placement = get(id);
    LocalDate on = date == null ? LocalDate.now(clock) : date;
    placement.place(
        currentUser.username(),
        on,
        p -> "RI:FAC:" + placement.getPlacementNo() + ":" + p.getParty().getCode());
    Cession cession = placement.getCession();
    PostingContext ctx =
        new PostingContext(
            placement.getCompanyId(),
            placement.getBranchId(),
            on,
            cession.getBusinessLine(),
            placement.getPlacementNo(),
            "Facultative placement " + placement.getPlacementNo() + " " + cession.getPolicyNo());
    for (FacParticipant p : placement.getParticipants()) {
      accounting.cede(
          ctx,
          p.getPostingRef(),
          p.getParty().getId(),
          cession.toBase(p.getPremium()),
          cession.toBase(p.getCommission()));
    }
    audit.record(ENTITY, placement.getPlacementNo(), AuditAction.AUTHORIZE, "Placed");
    return placement;
  }

  /**
   * Closes a placed slip.
   *
   * @param id placement
   * @param date closing date, null for today
   * @return placement
   */
  public FacPlacement close(Long id, LocalDate date) {
    FacPlacement placement = get(id);
    placement.close(date == null ? LocalDate.now(clock) : date);
    audit.record(ENTITY, placement.getPlacementNo(), AuditAction.CLOSE, "Closed slip");
    return placement;
  }
}
