package com.iortatechnxt.finverse.reinsurance.service;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.insurance.ClaimMovementType;
import com.iortatechnxt.finverse.reinsurance.domain.AmountPair;
import com.iortatechnxt.finverse.reinsurance.domain.CessionLineRepository;
import com.iortatechnxt.finverse.reinsurance.domain.RiClaimShareRepository;
import com.iortatechnxt.finverse.reinsurance.domain.Soa;
import com.iortatechnxt.finverse.reinsurance.domain.SoaFigures;
import com.iortatechnxt.finverse.reinsurance.domain.SoaPeriod;
import com.iortatechnxt.finverse.reinsurance.domain.SoaRepository;
import com.iortatechnxt.finverse.reinsurance.domain.Treaty;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyLayer;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyParticipant;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes the lines of a treaty participant's quarterly statement of account (base currency):
 *
 * <ul>
 *   <li>premium and commission: the participant's cession lines with an RI accounting date in the
 *       quarter; for an excess of loss treaty, a quarter of the minimum and deposit premium of the
 *       layers times the participant's share;
 *   <li>levy: premium x treaty levy %;
 *   <li>losses paid and recoveries: the participant's shares of claim payments and salvage in the
 *       quarter;
 *   <li>premium reserve retained: premium x participant premium reserve %; released: the amount
 *       retained in the same quarter of the previous year (held twelve months);
 *   <li>interest: reserves held at the start of the quarter x yearly interest % / 4;
 *   <li>outstanding loss reserve retained: the participant's share of outstanding claims at quarter
 *       end x treaty loss reserve %; released: the amount retained the previous quarter.
 * </ul>
 */
@Component
@Transactional(readOnly = true)
public class SoaCalculator {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final BigDecimal QUARTERS = BigDecimal.valueOf(4);

  private final CessionLineRepository lines;
  private final RiClaimShareRepository shares;
  private final SoaRepository statements;

  /**
   * Creates the calculator.
   *
   * @param lines cession lines
   * @param shares claim shares
   * @param statements earlier statements (reserves)
   */
  public SoaCalculator(
      CessionLineRepository lines, RiClaimShareRepository shares, SoaRepository statements) {
    this.lines = lines;
    this.shares = shares;
    this.statements = statements;
  }

  /**
   * Statement lines of a participant for a quarter.
   *
   * @param treaty treaty
   * @param participant participant
   * @param period quarter
   * @return figures
   */
  public SoaFigures compute(Treaty treaty, TreatyParticipant participant, SoaPeriod period) {
    Long treatyId = treaty.getId();
    Long partyId = participant.getParty().getId();
    AmountPair premiumAndCommission =
        treaty.getTreatyType() == TreatyType.XOL
            ? new AmountPair(depositInstalment(treaty, participant), Money.zero())
            : lines.premiumAndCommission(treatyId, partyId, period.from(), period.to());
    BigDecimal premium = premiumAndCommission.first();
    BigDecimal lossesPaid = total(treatyId, partyId, ClaimMovementType.PAYMENT, period);
    BigDecimal recoveries = total(treatyId, partyId, ClaimMovementType.RECOVERY, period).negate();
    List<Soa> history = statements.findByTreatyIdAndPartyId(treatyId, partyId);
    BigDecimal outstanding = Money.nz(shares.reserveShare(treatyId, partyId, period.to()));
    return new SoaFigures(
        premium,
        premiumAndCommission.second(),
        pct(premium, treaty.getLevyPct()),
        lossesPaid,
        recoveries,
        pct(premium, participant.getPremiumReservePct()),
        find(history, period.yearBefore())
            .map(SoaFigures::premiumReserveRetained)
            .orElse(Money.zero()),
        interest(history, period, treaty.getReserveInterestPct()),
        pct(outstanding, treaty.getLossReservePct()),
        find(history, period.previous()).map(SoaFigures::lossReserveRetained).orElse(Money.zero()));
  }

  private BigDecimal total(Long treatyId, Long partyId, ClaimMovementType type, SoaPeriod p) {
    return Money.round(Money.nz(shares.total(treatyId, partyId, type, p.from(), p.to())));
  }

  private static BigDecimal depositInstalment(Treaty treaty, TreatyParticipant participant) {
    BigDecimal mdp =
        treaty.getLayers().stream()
            .map(TreatyLayer::getMinDepositPremium)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return pct(
        mdp.divide(QUARTERS, Money.RATE_SCALE, RoundingMode.HALF_EVEN), participant.getSharePct());
  }

  private static BigDecimal interest(List<Soa> history, SoaPeriod period, BigDecimal ratePct) {
    BigDecimal held = Money.zero();
    for (Soa s : history) {
      if (before(s.period(), period)) {
        SoaFigures f = s.figures();
        held = held.add(f.premiumReserveNet()).add(f.lossReserveNet());
      }
    }
    return pct(
        held.max(Money.zero()).divide(QUARTERS, Money.RATE_SCALE, RoundingMode.HALF_EVEN), ratePct);
  }

  private static Optional<SoaFigures> find(List<Soa> history, SoaPeriod p) {
    return history.stream()
        .filter(s -> s.getSoaYear() == p.year() && s.getQuarter() == p.quarter())
        .map(Soa::figures)
        .findFirst();
  }

  private static boolean before(SoaPeriod a, SoaPeriod b) {
    return a.year() < b.year() || a.year() == b.year() && a.quarter() < b.quarter();
  }

  private static BigDecimal pct(BigDecimal amount, BigDecimal ratePct) {
    return Money.round(
        amount
            .multiply(Money.nz(ratePct))
            .divide(HUNDRED, Money.RATE_SCALE, RoundingMode.HALF_EVEN));
  }
}
