package com.iortatechnxt.finverse.reinsurance.service;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.reinsurance.domain.Cession;
import com.iortatechnxt.finverse.reinsurance.domain.CessionBasis;
import com.iortatechnxt.finverse.reinsurance.domain.CessionHeader;
import com.iortatechnxt.finverse.reinsurance.domain.CessionLine;
import com.iortatechnxt.finverse.reinsurance.domain.CessionRepository;
import com.iortatechnxt.finverse.reinsurance.domain.FacParticipant;
import com.iortatechnxt.finverse.reinsurance.domain.FacPlacement;
import com.iortatechnxt.finverse.reinsurance.domain.FacPlacementRepository;
import com.iortatechnxt.finverse.reinsurance.domain.Participation;
import com.iortatechnxt.finverse.reinsurance.domain.RiLayer;
import com.iortatechnxt.finverse.reinsurance.domain.RiskRef;
import com.iortatechnxt.finverse.reinsurance.domain.Treaty;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyParticipant;
import com.iortatechnxt.finverse.reinsurance.service.AllocationMath.SiSplit;
import com.iortatechnxt.finverse.reinsurance.service.CessionPlan.PlannedLine;
import com.iortatechnxt.finverse.underwriting.domain.EndorsementType;
import com.iortatechnxt.finverse.underwriting.domain.PremiumBreakdown;
import com.iortatechnxt.finverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.finverse.underwriting.service.PolicySnapshot;
import com.iortatechnxt.finverse.underwriting.service.PremiumTransaction;
import com.iortatechnxt.finverse.underwriting.service.RiskSnapshot;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes the allocation of a premium transaction without storing anything.
 *
 * <ul>
 *   <li><b>Full allocation</b> (original issue, renewal): the company net premium is spread over
 *       the insured risks by premium; each risk's company sum insured is split by {@link
 *       AllocationMath#split} on the treaty programme of the line of business and underwriting
 *       year; each treaty layer is shared among the treaty participants; the part beyond treaty
 *       capacity becomes a facultative requirement.
 *   <li><b>Pro-rata</b> (additional premium, refund, cancellation): ceded in the proportions of the
 *       full allocation in force at the effective date; a facultative share goes to the placement's
 *       participants when placed (the unplaced part is retained), otherwise it increases the
 *       provisional requirement.
 *   <li>Transactions without premium or sum insured (NIL endorsements) cede nothing.
 * </ul>
 */
@Component
@Transactional(readOnly = true)
public class CessionPlanner {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final PolicyQueryService policies;
  private final TreatyService treaties;
  private final CessionRepository cessions;
  private final FacPlacementRepository placements;

  /**
   * Creates the planner.
   *
   * @param policies policy risks
   * @param treaties treaty programmes
   * @param cessions allocations in force
   * @param placements facultative placements
   */
  public CessionPlanner(
      PolicyQueryService policies,
      TreatyService treaties,
      CessionRepository cessions,
      FacPlacementRepository placements) {
    this.policies = policies;
    this.treaties = treaties;
    this.cessions = cessions;
    this.placements = placements;
  }

  /**
   * Plans the allocation of a transaction.
   *
   * @param txn approved premium transaction
   * @return plan
   */
  public CessionPlan plan(PremiumTransaction txn) {
    CessionHeader header = header(txn);
    if (header.ourPremium().signum() == 0 && header.ourSi().signum() == 0) {
      return new CessionPlan(header, CessionBasis.NONE, List.of());
    }
    if (isFull(txn)) {
      return new CessionPlan(header, CessionBasis.FULL, full(header, txn.policy()));
    }
    return new CessionPlan(header, CessionBasis.PRO_RATA, proRata(header));
  }

  /**
   * Whether a transaction is allocated risk by risk on the programme.
   *
   * @param txn transaction
   * @return true for the original issue and renewals
   */
  static boolean isFull(PremiumTransaction txn) {
    return txn.ref().isOriginal() || EndorsementType.RENEWAL.name().equals(txn.kind());
  }

  private static CessionHeader header(PremiumTransaction txn) {
    PolicySnapshot p = txn.policy();
    PremiumBreakdown b = txn.premium();
    boolean renewal = EndorsementType.RENEWAL.name().equals(txn.kind());
    return new CessionHeader(
        p.companyId(),
        p.branchId(),
        p.id(),
        p.policyNo(),
        txn.endorsementNo(),
        txn.documentNo(),
        txn.kind(),
        p.businessLine(),
        p.productCode(),
        p.uwYear(),
        renewal ? txn.effectiveDate().getYear() : p.uwYear(),
        txn.issueDate(),
        txn.effectiveDate(),
        txn.approvalDate(),
        p.currency(),
        txn.exchangeRate() == null ? BigDecimal.ONE : txn.exchangeRate(),
        p.sharePct(),
        b.getOurSumInsured(),
        b.getOurNetPremium());
  }

  private List<PlannedLine> full(CessionHeader h, PolicySnapshot policy) {
    TreatyProgramme programme = treaties.programme(h.companyId(), h.businessLine(), h.treatyYear());
    List<RiskSnapshot> risks = policies.risks(policy.id());
    List<BigDecimal> premiums =
        AllocationMath.prorate(h.ourPremium(), risks.stream().map(RiskSnapshot::premium).toList());
    List<PlannedLine> out = new ArrayList<>();
    for (int i = 0; i < risks.size(); i++) {
      RiskSnapshot r = risks.get(i);
      BigDecimal ourSi = AllocationMath.proportion(r.sumInsured(), h.sharePct(), HUNDRED);
      RiskRef risk = new RiskRef(r.id(), r.lineNo(), r.description(), ourSi, premiums.get(i));
      out.addAll(allocateRisk(risk, programme, h.exchangeRate()));
    }
    return out;
  }

  /**
   * Allocates one risk on a programme.
   *
   * @param risk risk (company sum insured and premium, policy currency)
   * @param programme treaty programme (limits in base currency)
   * @param rate exchange rate of the policy currency to the base currency
   * @return planned shares, the retention first
   */
  static List<PlannedLine> allocateRisk(RiskRef risk, TreatyProgramme programme, BigDecimal rate) {
    SiSplit split =
        AllocationMath.split(risk.ourSi().max(Money.zero()), programme.capacity().inCurrency(rate));
    BigDecimal si = risk.ourSi().max(Money.zero());
    List<PlannedLine> ceded = new ArrayList<>();
    ceded.addAll(treatyShares(risk, programme.quotaShare(), split.quotaShare(), si));
    ceded.addAll(treatyShares(risk, programme.surplus(), split.surplus(), si));
    if (split.fac().signum() > 0) {
      ceded.add(
          new PlannedLine(
              risk,
              new Participation(RiLayer.FAC, null, null, null, null, BigDecimal.ZERO),
              split.fac(),
              AllocationMath.proportion(risk.ourPremium(), split.fac(), si)));
    }
    return withRetention(risk, split.retention(), ceded);
  }

  private static List<PlannedLine> treatyShares(
      RiskRef risk, Treaty treaty, BigDecimal layerSi, BigDecimal riskSi) {
    if (treaty == null || layerSi.signum() == 0) {
      return List.of();
    }
    BigDecimal layerPremium = AllocationMath.proportion(risk.ourPremium(), layerSi, riskSi);
    List<TreatyParticipant> parts = treaty.getParticipants();
    List<BigDecimal> shares = parts.stream().map(TreatyParticipant::getSharePct).toList();
    List<BigDecimal> sis = AllocationMath.prorate(layerSi, shares);
    List<BigDecimal> premiums = AllocationMath.prorate(layerPremium, shares);
    List<PlannedLine> out = new ArrayList<>();
    for (int i = 0; i < parts.size(); i++) {
      TreatyParticipant p = parts.get(i);
      Participation who =
          new Participation(
              RiLayer.of(treaty.getTreatyType()),
              treaty.getId(),
              null,
              p.getParty().getId(),
              p.getParty().getCode(),
              p.getCommissionPct());
      out.add(new PlannedLine(risk, who, sis.get(i), premiums.get(i)));
    }
    return out;
  }

  private static List<PlannedLine> withRetention(
      RiskRef risk, BigDecimal retentionSi, List<PlannedLine> ceded) {
    BigDecimal cededPremium =
        ceded.stream().map(PlannedLine::premium).reduce(Money.zero(), BigDecimal::add);
    List<PlannedLine> out = new ArrayList<>();
    out.add(
        new PlannedLine(
            risk,
            Participation.retention(),
            retentionSi,
            Money.round(risk.ourPremium()).subtract(cededPremium)));
    out.addAll(ceded);
    return out;
  }

  private List<PlannedLine> proRata(CessionHeader h) {
    Cession base = inForce(h);
    Map<Long, List<CessionLine>> byRisk = new LinkedHashMap<>();
    base.getLines()
        .forEach(l -> byRisk.computeIfAbsent(l.getRiskId(), k -> new ArrayList<>()).add(l));
    List<RiskRef> baseRisks = byRisk.values().stream().map(l -> l.get(0).risk()).toList();
    List<BigDecimal> premiums =
        AllocationMath.prorate(h.ourPremium(), weights(baseRisks, RiskRef::ourPremium));
    List<BigDecimal> sis = AllocationMath.prorate(h.ourSi(), weights(baseRisks, RiskRef::ourSi));
    Map<Long, FacPlacement> facs = placementsOf(base);
    List<PlannedLine> out = new ArrayList<>();
    int i = 0;
    for (List<CessionLine> lines : byRisk.values()) {
      RiskRef base0 = lines.get(0).risk();
      RiskRef risk =
          new RiskRef(
              base0.riskId(), base0.lineNo(), base0.description(), sis.get(i), premiums.get(i));
      out.addAll(proRataRisk(risk, lines, facs));
      i++;
    }
    return out;
  }

  private Cession inForce(CessionHeader h) {
    List<Cession> full = cessions.fullAllocations(h.policyId());
    return full.stream()
        .filter(c -> !c.getEffectiveDate().isAfter(h.effectiveDate()))
        .findFirst()
        .or(() -> full.stream().reduce((a, b) -> b))
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "NO_BASE_ALLOCATION",
                    "Policy " + h.policyNo() + " has no allocation to cede endorsements against"));
  }

  private Map<Long, FacPlacement> placementsOf(Cession base) {
    Map<Long, FacPlacement> out = new LinkedHashMap<>();
    placements.findByCessionIdIn(List.of(base.getId())).forEach(p -> out.put(p.getId(), p));
    return out;
  }

  private static List<BigDecimal> weights(List<RiskRef> risks, Function<RiskRef, BigDecimal> f) {
    return risks.stream().map(f).map(v -> v.max(Money.zero())).toList();
  }

  private static List<PlannedLine> proRataRisk(
      RiskRef risk, List<CessionLine> baseLines, Map<Long, FacPlacement> facs) {
    List<PlannedLine> ceded = new ArrayList<>();
    BigDecimal cededSi = Money.zero();
    for (CessionLine l : baseLines) {
      if (l.getLayer() == RiLayer.RETENTION) {
        continue;
      }
      List<PlannedLine> shares =
          l.getLayer() == RiLayer.FAC && !l.participation().isCeded()
              ? facShares(risk, l, facs.get(l.getPlacementId()))
              : List.of(share(risk, l.participation(), l.getSharePct()));
      ceded.addAll(shares);
      cededSi =
          cededSi.add(
              shares.stream().map(PlannedLine::sumInsured).reduce(Money.zero(), BigDecimal::add));
    }
    return withRetention(risk, Money.round(risk.ourSi()).subtract(cededSi), ceded);
  }

  private static List<PlannedLine> facShares(RiskRef risk, CessionLine l, FacPlacement placement) {
    if (placement == null || !placement.getStatus().isPlaced()) {
      return List.of(share(risk, l.participation(), l.getSharePct()));
    }
    List<PlannedLine> out = new ArrayList<>();
    for (FacParticipant p : placement.getParticipants()) {
      BigDecimal pct =
          l.getSharePct()
              .multiply(p.getSharePct())
              .divide(HUNDRED, Money.RATE_SCALE, Money.ROUNDING);
      Participation who =
          new Participation(
              RiLayer.FAC,
              null,
              placement.getId(),
              p.getParty().getId(),
              p.getParty().getCode(),
              p.getCommissionPct());
      out.add(share(risk, who, pct));
    }
    return out;
  }

  private static PlannedLine share(RiskRef risk, Participation who, BigDecimal pct) {
    return new PlannedLine(
        risk,
        who,
        AllocationMath.proportion(risk.ourSi(), pct, HUNDRED),
        AllocationMath.proportion(risk.ourPremium(), pct, HUNDRED));
  }
}
