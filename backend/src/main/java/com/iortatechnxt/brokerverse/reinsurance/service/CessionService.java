package com.iortatechnxt.brokerverse.reinsurance.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.reinsurance.domain.Cession;
import com.iortatechnxt.brokerverse.reinsurance.domain.CessionBasis;
import com.iortatechnxt.brokerverse.reinsurance.domain.CessionHeader;
import com.iortatechnxt.brokerverse.reinsurance.domain.CessionLine;
import com.iortatechnxt.brokerverse.reinsurance.domain.CessionRepository;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacPlacement;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacPlacementRepository;
import com.iortatechnxt.brokerverse.reinsurance.domain.Participation;
import com.iortatechnxt.brokerverse.reinsurance.domain.RiLayer;
import com.iortatechnxt.brokerverse.reinsurance.service.CessionPlan.PlannedLine;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.brokerverse.underwriting.service.PremiumTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cedes approved premium transactions: stores the allocation planned by {@link CessionPlanner},
 * creates facultative placements for requirements beyond treaty capacity and accounts for the
 * premium ceded to each reinsurer ({@code RI_PREMIUM_CEDED}, one event and open item per treaty or
 * placement and participant).
 *
 * <p>Idempotent per transaction: a transaction already ceded returns its existing cession. An
 * endorsement whose policy was never ceded has the earlier transactions ceded first, so pro-rata
 * cessions always find the allocation in force.
 */
@Service
@Transactional
public class CessionService {

  static final String ENTITY = "Cession";

  /** Exception code raised when a risk exceeds the treaty capacity. */
  static final String CAPACITY_ALERT = "RI_TREATY_CAPACITY";

  private final CessionPlanner planner;
  private final CessionRepository cessions;
  private final FacPlacementRepository placements;
  private final PolicyQueryService policies;
  private final ReinsuranceAccounting accounting;
  private final ReinsuranceNumbers numbers;
  private final AlertService alerts;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param planner allocation planner
   * @param cessions cessions
   * @param placements facultative placements
   * @param policies premium transactions of a policy
   * @param accounting reinsurance postings
   * @param numbers document numbers
   * @param alerts exception alerts
   * @param audit audit trail
   */
  public CessionService(
      CessionPlanner planner,
      CessionRepository cessions,
      FacPlacementRepository placements,
      PolicyQueryService policies,
      ReinsuranceAccounting accounting,
      ReinsuranceNumbers numbers,
      AlertService alerts,
      AuditTrailService audit) {
    this.planner = planner;
    this.cessions = cessions;
    this.placements = placements;
    this.policies = policies;
    this.accounting = accounting;
    this.numbers = numbers;
    this.alerts = alerts;
    this.audit = audit;
  }

  /**
   * Gets a cession with its lines.
   *
   * @param id id
   * @return cession
   */
  @Transactional(readOnly = true)
  public Cession get(Long id) {
    return cessions
        .findWithLinesById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Cessions of a policy.
   *
   * @param policyId policy
   * @return cessions in endorsement order
   */
  @Transactional(readOnly = true)
  public List<Cession> ofPolicy(Long policyId) {
    return cessions.findByPolicyIdOrderByEndorsementNo(policyId);
  }

  /**
   * Cessions of a policy found by number.
   *
   * @param companyId company
   * @param policyNo policy number
   * @return cessions in endorsement order (empty when the policy is unknown)
   */
  @Transactional(readOnly = true)
  public List<Cession> ofPolicyNumber(Long companyId, String policyNo) {
    return policies.findByNumber(companyId, policyNo).map(p -> ofPolicy(p.id())).orElse(List.of());
  }

  /**
   * Cessions with an RI accounting date in a period.
   *
   * @param companyId company
   * @param from first date
   * @param to last date
   * @return cessions with lines
   */
  @Transactional(readOnly = true)
  public List<Cession> list(Long companyId, LocalDate from, LocalDate to) {
    return cessions.findByCompanyIdAndRiDateBetweenOrderByRiDateAscCessionNoAsc(
        companyId, from, to);
  }

  /**
   * Cedes every approved transaction of a policy not yet ceded (on-demand allocation).
   *
   * @param policyId policy
   * @return all cessions of the policy
   */
  public List<Cession> cedePolicy(Long policyId) {
    policies.policyTransactions(policyId).forEach(this::cedeTransaction);
    return ofPolicy(policyId);
  }

  /**
   * Cedes one approved transaction (idempotent); earlier transactions of the policy not yet ceded
   * are ceded first.
   *
   * @param txn approved premium transaction
   * @return cession
   */
  public Cession cede(PremiumTransaction txn) {
    if (!txn.ref().isOriginal() && !isCeded(txn.policy().companyId(), txn.ref().policyId(), 0)) {
      policies.policyTransactions(txn.ref().policyId()).stream()
          .filter(t -> t.endorsementNo() < txn.endorsementNo())
          .forEach(this::cedeTransaction);
    }
    return cedeTransaction(txn);
  }

  private Cession cedeTransaction(PremiumTransaction txn) {
    Optional<Cession> existing =
        cessions.findByCompanyIdAndPolicyIdAndEndorsementNo(
            txn.policy().companyId(), txn.ref().policyId(), txn.endorsementNo());
    return existing.orElseGet(() -> store(planner.plan(txn)));
  }

  private boolean isCeded(Long companyId, Long policyId, int endorsementNo) {
    return cessions
        .findByCompanyIdAndPolicyIdAndEndorsementNo(companyId, policyId, endorsementNo)
        .isPresent();
  }

  private Cession store(CessionPlan plan) {
    CessionHeader h = plan.header();
    Cession cession =
        cessions.save(new Cession(numbers.cession(h.branchId(), h.riDate()), plan.basis(), h));
    for (PlannedLine line : plan.lines()) {
      Participation who = facRequirement(cession, line);
      cession.addLine(
          new CessionLine(cession, line.risk(), who, line.sumInsured(), line.premium()));
    }
    Cession saved = cessions.save(cession);
    post(saved);
    if (plan.basis() == CessionBasis.FULL && plan.siOf(RiLayer.FAC).signum() > 0) {
      alerts.raise(
          CAPACITY_ALERT,
          new AlertFacts(
              h.companyId(),
              h.branchId(),
              ENTITY,
              saved.getCessionNo(),
              "Policy "
                  + h.policyNo()
                  + ": sum insured "
                  + plan.siOf(RiLayer.FAC)
                  + " exceeds the "
                  + h.businessLine()
                  + " "
                  + h.treatyYear()
                  + " treaty capacity; facultative cover required",
              saved.toBase(plan.premiumOf(RiLayer.FAC)),
              CAPACITY_ALERT + ":" + saved.getCessionNo()));
    }
    audit.record(
        ENTITY,
        saved.getCessionNo(),
        AuditAction.POST,
        "Ceded " + h.documentNo() + " (" + plan.basis() + ")");
    return saved;
  }

  /**
   * Resolves the facultative placement of a planned FAC share without a reinsurer: a new
   * requirement creates a provisional placement; an endorsement adds to the existing one.
   */
  private Participation facRequirement(Cession cession, PlannedLine line) {
    Participation who = line.who();
    if (who.layer() != RiLayer.FAC || who.isCeded()) {
      return who;
    }
    FacPlacement placement;
    if (who.placementId() == null) {
      placement =
          placements.save(
              new FacPlacement(
                  numbers.placement(cession.getBranchId(), cession.getRiDate()),
                  cession,
                  line.risk(),
                  line.sumInsured(),
                  line.premium()));
    } else {
      placement = placements.getReferenceById(who.placementId());
      placement.addProvisional(line.sumInsured(), line.premium());
    }
    return new Participation(RiLayer.FAC, null, placement.getId(), null, null, BigDecimal.ZERO);
  }

  /** Posts the premium ceded per contract and reinsurer. */
  private void post(Cession cession) {
    Map<String, List<CessionLine>> groups = new LinkedHashMap<>();
    for (CessionLine l : cession.getLines()) {
      Participation who = l.participation();
      if (who.isCeded()) {
        String ref =
            "RI:CES:" + cession.getCessionNo() + ":" + who.contractKey() + ":" + who.partyCode();
        groups.computeIfAbsent(ref, k -> new ArrayList<>()).add(l);
      }
    }
    PostingContext ctx =
        new PostingContext(
            cession.getCompanyId(),
            cession.getBranchId(),
            cession.getRiDate(),
            cession.getBusinessLine(),
            cession.getCessionNo(),
            "Reinsurance cession " + cession.getDocumentNo());
    groups.forEach(
        (ref, lines) -> {
          BigDecimal premium = sum(lines, CessionLine::getBasePremium);
          BigDecimal commission = sum(lines, CessionLine::getBaseCommission);
          accounting.cede(ctx, ref, lines.get(0).getPartyId(), premium, commission);
          lines.forEach(l -> l.markPosted(ref));
        });
  }

  private static BigDecimal sum(List<CessionLine> lines, Function<CessionLine, BigDecimal> f) {
    return lines.stream().map(f).reduce(Money.zero(), BigDecimal::add);
  }
}
