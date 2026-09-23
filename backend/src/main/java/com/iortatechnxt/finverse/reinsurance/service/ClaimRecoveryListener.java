package com.iortatechnxt.finverse.reinsurance.service;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.insurance.ClaimMovement;
import com.iortatechnxt.finverse.insurance.ClaimMovementListener;
import com.iortatechnxt.finverse.insurance.ClaimMovementType;
import com.iortatechnxt.finverse.reinsurance.domain.Cession;
import com.iortatechnxt.finverse.reinsurance.domain.Participation;
import com.iortatechnxt.finverse.reinsurance.domain.RiClaimMovement;
import com.iortatechnxt.finverse.reinsurance.domain.RiClaimMovementRepository;
import com.iortatechnxt.finverse.reinsurance.domain.RiClaimShare;
import com.iortatechnxt.finverse.reinsurance.domain.RiClaimShareRepository;
import com.iortatechnxt.finverse.reinsurance.domain.RiLayer;
import com.iortatechnxt.finverse.reinsurance.domain.Treaty;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyLayer;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyParticipant;
import com.iortatechnxt.finverse.reinsurance.service.ClaimShareCalculator.ShareFraction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the kernel port {@link ClaimMovementListener}: books the reinsurers' share of every
 * posted claim movement, inside the claims transaction.
 *
 * <ul>
 *   <li>RESERVE_CHANGE: reinsurers' share of the change (percentages of the allocation in force at
 *       the loss date), {@code RI_RESERVE_SHARE} (Dr reinsurers' share of claims reserves / Cr
 *       reinsurers' share of claims).
 *   <li>PAYMENT: recovery due from each reinsurer, {@code RI_CLAIM_RECOVERY} and a DEBIT open item
 *       on the reinsurer; the company's net retained part is recovered from the excess of loss
 *       treaty of the line and loss year above the priority, per claim, within the layer limit and
 *       the yearly aggregate (limit x (1 + reinstatements)).
 *   <li>RECOVERY (salvage, subrogation): the same shares reversed (the reinsurers' recovery due and
 *       the net retained loss both reduce).
 * </ul>
 *
 * Idempotent on the movement reference: a reference already processed is ignored.
 */
@Service
@Transactional
public class ClaimRecoveryListener implements ClaimMovementListener {

  private final RiClaimMovementRepository movements;
  private final RiClaimShareRepository shares;
  private final ClaimShareCalculator calculator;
  private final TreatyService treaties;
  private final ReinsuranceAccounting accounting;

  /**
   * Creates the listener.
   *
   * @param movements processed movements
   * @param shares shares (excess of loss totals)
   * @param calculator proportional percentages
   * @param treaties excess of loss programme
   * @param accounting reinsurance postings
   */
  public ClaimRecoveryListener(
      RiClaimMovementRepository movements,
      RiClaimShareRepository shares,
      ClaimShareCalculator calculator,
      TreatyService treaties,
      ReinsuranceAccounting accounting) {
    this.movements = movements;
    this.shares = shares;
    this.calculator = calculator;
    this.treaties = treaties;
    this.accounting = accounting;
  }

  @Override
  public void onClaimMovement(ClaimMovement movement) {
    if (movements.existsByCompanyIdAndReference(movement.companyId(), movement.reference())) {
      return;
    }
    Optional<Cession> cession = calculator.inForce(movement.policyId(), movement.lossDate());
    RiClaimMovement row = new RiClaimMovement(movement, cession.map(Cession::getId).orElse(null));
    List<ShareFraction> fractions = cession.map(calculator::fractions).orElse(List.of());
    BigDecimal loss = row.signedLoss();
    List<BigDecimal> amounts =
        fractions.stream().map(f -> Money.round(loss.multiply(f.fraction()))).toList();
    for (int i = 0; i < fractions.size(); i++) {
      row.addShare(
          new RiClaimShare(
              row, fractions.get(i).who(), null, fractions.get(i).percent(), amounts.get(i)));
    }
    if (movement.type() != ClaimMovementType.RESERVE_CHANGE) {
      row.retain(loss.subtract(row.ceded()));
      excessOfLoss(row);
    }
    RiClaimMovement saved = movements.save(row);
    post(saved);
  }

  private void excessOfLoss(RiClaimMovement row) {
    Treaty xol =
        treaties
            .programme(row.getCompanyId(), row.getBusinessLine(), row.getLossDate().getYear())
            .excessOfLoss();
    if (xol == null) {
      return;
    }
    BigDecimal before = Money.nz(movements.netRetained(row.getClaimId()));
    BigDecimal after = before.add(row.getNetRetained());
    for (TreatyLayer layer : xol.getLayers()) {
      BigDecimal target =
          AllocationMath.excessOfLoss(after, layer.getPriority(), layer.getLayerLimit());
      BigDecimal claimSoFar =
          Money.nz(shares.claimLayerTotal(row.getClaimId(), xol.getId(), layer.getLayerNo()));
      BigDecimal used = Money.nz(shares.layerTotal(xol.getId(), layer.getLayerNo()));
      BigDecimal remaining = layer.aggregateLimit().subtract(used).max(Money.zero());
      BigDecimal delta = target.min(claimSoFar.add(remaining)).subtract(claimSoFar);
      if (delta.signum() != 0) {
        addLayerShares(row, xol, layer, delta);
      }
    }
  }

  private static void addLayerShares(
      RiClaimMovement row, Treaty xol, TreatyLayer layer, BigDecimal delta) {
    List<TreatyParticipant> parts = xol.getParticipants();
    List<BigDecimal> split =
        AllocationMath.prorate(delta, parts.stream().map(TreatyParticipant::getSharePct).toList());
    for (int i = 0; i < parts.size(); i++) {
      TreatyParticipant p = parts.get(i);
      Participation who =
          new Participation(
              RiLayer.XOL,
              xol.getId(),
              null,
              p.getParty().getId(),
              p.getParty().getCode(),
              BigDecimal.ZERO);
      row.addShare(new RiClaimShare(row, who, layer.getLayerNo(), p.getSharePct(), split.get(i)));
    }
  }

  private void post(RiClaimMovement row) {
    PostingContext ctx =
        new PostingContext(
            row.getCompanyId(),
            row.getBranchId(),
            row.getMovementDate(),
            row.getBusinessLine(),
            row.getClaimNo(),
            "Reinsurers' share of claim " + row.getClaimNo());
    String base = "RI:CLM:" + row.getId();
    if (row.getMovementType() == ClaimMovementType.RESERVE_CHANGE) {
      BigDecimal total = row.ceded();
      if (total.signum() != 0) {
        accounting.reserveShare(ctx, base, total);
        row.getShares().forEach(s -> s.markPosted(base));
      }
      return;
    }
    Map<String, List<RiClaimShare>> groups = new LinkedHashMap<>();
    for (RiClaimShare s : row.getShares()) {
      String ref = base + ":" + s.contractKey() + ":" + s.getPartyCode();
      groups.computeIfAbsent(ref, k -> new ArrayList<>()).add(s);
    }
    groups.forEach(
        (ref, list) -> {
          BigDecimal amount =
              list.stream().map(RiClaimShare::getBaseAmount).reduce(Money.zero(), BigDecimal::add);
          if (amount.signum() != 0) {
            accounting.recovery(ctx, ref, list.get(0).getPartyId(), amount);
            list.forEach(s -> s.markPosted(ref));
          }
        });
  }
}
