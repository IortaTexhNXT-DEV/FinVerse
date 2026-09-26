package com.iortatechnxt.brokerverse.claims.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.domain.ClaimPartyRole;
import com.iortatechnxt.brokerverse.claims.domain.Lpo;
import com.iortatechnxt.brokerverse.claims.domain.LpoRepository;
import com.iortatechnxt.brokerverse.claims.domain.LpoTerms;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Local purchase orders for motor repairs: issued to a garage under an active motor claim (the
 * garage is added to the claim's parties), numbered {@code LPO-<branch>-<year>-000001}, cancellable
 * with a reason. LPOs carry no accounting; the garage is paid through a settlement.
 */
@Service
@Transactional
public class LpoService {

  static final String ENTITY = "Lpo";

  /** Line of business of motor claims. */
  static final String MOTOR = "MOTOR";

  private final LpoRepository lpos;
  private final PartyService parties;
  private final ClaimSupport support;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param lpos LPO repository
   * @param parties garage lookup
   * @param support claims helpers
   * @param audit audit trail
   */
  public LpoService(
      LpoRepository lpos, PartyService parties, ClaimSupport support, AuditTrailService audit) {
    this.lpos = lpos;
    this.parties = parties;
    this.support = support;
    this.audit = audit;
  }

  /**
   * LPOs of a claim.
   *
   * @param claimId claim
   * @return LPOs with garage
   */
  @Transactional(readOnly = true)
  public List<Lpo> forClaim(Long claimId) {
    return lpos.findByClaimIdOrderById(claimId);
  }

  /**
   * LPOs of a company (LPO register).
   *
   * @param companyId company
   * @return LPOs with claim and garage, newest first
   */
  @Transactional(readOnly = true)
  public List<Lpo> forCompany(Long companyId) {
    return lpos.findByCompany(companyId);
  }

  /**
   * Issues an LPO.
   *
   * @param claimId motor claim
   * @param command garage, cover, date, amounts and description
   * @return issued LPO
   */
  public Lpo issue(Long claimId, LpoCommand command) {
    Claim claim = support.claim(claimId);
    claim.requireActive("issue an LPO for");
    if (!MOTOR.equals(claim.getPolicy().getBusinessLine())) {
      throw new BusinessRuleException("LPO_MOTOR_ONLY", "LPOs are issued for motor claims only");
    }
    Party garage =
        parties.requireActive(
            claim.getCompanyId(), command.garageCode(), ClaimPartyRole.GARAGE.partyTypes());
    if (!claim.involves(garage, ClaimPartyRole.GARAGE)) {
      claim.addParty(garage, ClaimPartyRole.GARAGE);
    }
    LpoTerms terms =
        new LpoTerms(
            garage,
            command.cover(),
            support.dateOrToday(command.issueDate()),
            command.gross(),
            command.discount(),
            command.description());
    Lpo lpo =
        lpos.save(
            new Lpo(
                claim, support.nextNumber("LPO", claim.getBranchId(), terms.issueDate()), terms));
    audit.record(
        ENTITY,
        lpo.getLpoNo(),
        AuditAction.CREATE,
        "Issued to " + garage.getCode() + " for " + lpo.getNetAmount());
    return lpo;
  }

  /**
   * Cancels an LPO.
   *
   * @param id LPO
   * @param reason reason
   * @return cancelled LPO
   */
  public Lpo cancel(Long id, String reason) {
    Lpo lpo =
        lpos.findWithDetailsById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    lpo.cancel(reason);
    audit.record(ENTITY, lpo.getLpoNo(), AuditAction.DEACTIVATE, reason);
    return lpo;
  }
}
