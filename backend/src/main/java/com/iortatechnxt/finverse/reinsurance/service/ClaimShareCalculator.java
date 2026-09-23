package com.iortatechnxt.finverse.reinsurance.service;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.reinsurance.domain.Cession;
import com.iortatechnxt.finverse.reinsurance.domain.CessionLine;
import com.iortatechnxt.finverse.reinsurance.domain.CessionRepository;
import com.iortatechnxt.finverse.reinsurance.domain.FacParticipant;
import com.iortatechnxt.finverse.reinsurance.domain.FacPlacement;
import com.iortatechnxt.finverse.reinsurance.domain.FacPlacementRepository;
import com.iortatechnxt.finverse.reinsurance.domain.Participation;
import com.iortatechnxt.finverse.reinsurance.domain.RiLayer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proportional reinsurance percentages of a claim: those of the full allocation of the policy in
 * force at the loss date (same percentages as the premium). Each quota share, surplus and placed
 * facultative participant takes its sum insured divided by the company sum insured; the company
 * retains the rest (including any facultative requirement not placed).
 */
@Component
@Transactional(readOnly = true)
public class ClaimShareCalculator {

  private static final int FRACTION_SCALE = 10;

  private final CessionRepository cessions;
  private final FacPlacementRepository placements;

  /**
   * Creates the calculator.
   *
   * @param cessions allocations
   * @param placements facultative placements
   */
  public ClaimShareCalculator(CessionRepository cessions, FacPlacementRepository placements) {
    this.cessions = cessions;
    this.placements = placements;
  }

  /**
   * Full allocation in force at a loss date: the latest one effective on or before the date, or the
   * earliest one when the loss predates them all.
   *
   * @param policyId policy
   * @param lossDate date of loss
   * @return cession, empty when the policy was never ceded
   */
  public Optional<Cession> inForce(Long policyId, LocalDate lossDate) {
    List<Cession> full = cessions.fullAllocations(policyId);
    return full.stream()
        .filter(c -> !c.getEffectiveDate().isAfter(lossDate))
        .findFirst()
        .or(() -> full.stream().reduce((a, b) -> b));
  }

  /**
   * Share of each reinsurer in a loss under an allocation.
   *
   * @param cession full allocation in force
   * @return fractions (0 to 1) of the company loss, per contract and reinsurer
   */
  public List<ShareFraction> fractions(Cession cession) {
    BigDecimal ourSi = cession.getOurSi();
    if (ourSi.signum() <= 0) {
      return List.of();
    }
    Map<String, Participation> who = new LinkedHashMap<>();
    Map<String, BigDecimal> si = new LinkedHashMap<>();
    for (CessionLine l : cession.getLines()) {
      Participation p = l.participation();
      if (l.getLayer().isProportionalTreaty() && p.isCeded()) {
        add(who, si, p, l.getSumInsured());
      }
    }
    for (FacPlacement f : placements.findByCessionIdIn(List.of(cession.getId()))) {
      if (f.getStatus().isPlaced()) {
        for (FacParticipant fp : f.getParticipants()) {
          Participation p =
              new Participation(
                  RiLayer.FAC,
                  null,
                  f.getId(),
                  fp.getParty().getId(),
                  fp.getParty().getCode(),
                  fp.getCommissionPct());
          add(who, si, p, fp.getSumInsured());
        }
      }
    }
    List<ShareFraction> out = new ArrayList<>();
    who.forEach(
        (key, p) ->
            out.add(
                new ShareFraction(
                    p, si.get(key).divide(ourSi, FRACTION_SCALE, RoundingMode.HALF_EVEN))));
    return out;
  }

  private static void add(
      Map<String, Participation> who, Map<String, BigDecimal> si, Participation p, BigDecimal v) {
    String key = p.contractKey() + ":" + p.partyId();
    who.putIfAbsent(key, p);
    si.merge(key, Money.round(v), BigDecimal::add);
  }

  /**
   * A reinsurer's fraction of a loss.
   *
   * @param who contract and reinsurer
   * @param fraction share of the company loss (0 to 1)
   */
  public record ShareFraction(Participation who, BigDecimal fraction) {

    /**
     * Share as a percentage.
     *
     * @return fraction x 100
     */
    public BigDecimal percent() {
      return fraction.multiply(BigDecimal.valueOf(100)).setScale(Money.RATE_SCALE, Money.ROUNDING);
    }
  }
}
