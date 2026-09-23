package com.iortatechnxt.finverse.claims.service;

import com.iortatechnxt.finverse.claims.domain.Claim;
import com.iortatechnxt.finverse.claims.domain.EstimateSide;
import com.iortatechnxt.finverse.claims.domain.MovementKind;
import com.iortatechnxt.finverse.claims.domain.MovementLine;
import com.iortatechnxt.finverse.insurance.ClaimMovement;
import com.iortatechnxt.finverse.insurance.ClaimMovementType;
import java.math.BigDecimal;
import java.util.List;

/**
 * Translates claim movement lines into the movements of the insurance shared kernel, company share:
 *
 * <table>
 *   <caption>Line to kernel movements</caption>
 *   <tr><th>Line</th><th>Kernel movements (reference)</th></tr>
 *   <tr><td>payment estimate change</td><td>RESERVE_CHANGE ± delta (line reference)</td></tr>
 *   <tr><td>settlement</td><td>PAYMENT + amount (line reference) and RESERVE_CHANGE − amount
 *       (line reference + ":RSV"), because a payment consumes the reserve</td></tr>
 *   <tr><td>recovery received</td><td>RECOVERY + amount (line reference)</td></tr>
 *   <tr><td>recovery estimate change</td><td>none (memorandum estimate)</td></tr>
 * </table>
 *
 * <p>So the running sum of RESERVE_CHANGE movements always equals the outstanding reserve. The
 * translation is deterministic: listener notifications and {@link ClaimsExperienceService} return
 * the same references.
 */
public final class KernelMovements {

  /** Suffix of the reserve consumption that accompanies a payment. */
  public static final String CONSUMPTION_SUFFIX = ":RSV";

  private KernelMovements() {}

  /**
   * Kernel movements of a line.
   *
   * @param line movement line (claim loaded)
   * @return movements, possibly empty
   */
  public static List<ClaimMovement> of(MovementLine line) {
    boolean payment = line.getSide() == EstimateSide.PAYMENT;
    if (line.getKind() == MovementKind.ESTIMATE) {
      return payment
          ? List.of(movement(line, ClaimMovementType.RESERVE_CHANGE, false, ""))
          : List.of();
    }
    if (!payment) {
      return List.of(movement(line, ClaimMovementType.RECOVERY, false, ""));
    }
    return List.of(
        movement(line, ClaimMovementType.PAYMENT, false, ""),
        movement(line, ClaimMovementType.RESERVE_CHANGE, true, CONSUMPTION_SUFFIX));
  }

  private static ClaimMovement movement(
      MovementLine line, ClaimMovementType type, boolean negate, String suffix) {
    Claim c = line.getClaim();
    BigDecimal amount = negate ? line.getAmount().negate() : line.getAmount();
    BigDecimal base = negate ? line.getBaseAmount().negate() : line.getBaseAmount();
    return new ClaimMovement(
        c.getCompanyId(),
        c.getBranchId(),
        c.getId(),
        c.getClaimNo(),
        c.getPolicy().getPolicyId(),
        c.getPolicy().getBusinessLine(),
        c.getLoss().getLossDate(),
        line.getMovementDate(),
        type,
        line.getCurrency(),
        amount,
        base,
        line.getReference() + suffix);
  }
}
