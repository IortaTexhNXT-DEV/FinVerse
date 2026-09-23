package com.iortatechnxt.finverse.reinsurance.api.dto;

import com.iortatechnxt.finverse.insurance.ClaimMovementType;
import com.iortatechnxt.finverse.reinsurance.domain.RiClaimMovement;
import com.iortatechnxt.finverse.reinsurance.domain.RiClaimShare;
import com.iortatechnxt.finverse.reinsurance.domain.RiLayer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A claim movement and the reinsurers' shares booked for it (base currency).
 *
 * @param id id
 * @param reference movement reference
 * @param claimId claim
 * @param claimNo claim number
 * @param policyId policy
 * @param businessLine line of business
 * @param lossDate date of loss
 * @param movementDate movement date
 * @param movementType type
 * @param currency claim currency
 * @param amount amount in claim currency
 * @param baseAmount base currency amount
 * @param ceded reinsurers' total share
 * @param netRetained company net retained (payments and recoveries)
 * @param shares reinsurers' shares
 */
public record ClaimMovementResponse(
    Long id,
    String reference,
    Long claimId,
    String claimNo,
    Long policyId,
    String businessLine,
    LocalDate lossDate,
    LocalDate movementDate,
    ClaimMovementType movementType,
    String currency,
    BigDecimal amount,
    BigDecimal baseAmount,
    BigDecimal ceded,
    BigDecimal netRetained,
    List<Share> shares) {

  /** Canonical constructor copying the shares. */
  public ClaimMovementResponse {
    shares = List.copyOf(shares);
  }

  /**
   * Maps a movement (shares loaded).
   *
   * @param m movement
   * @return response
   */
  public static ClaimMovementResponse from(RiClaimMovement m) {
    return new ClaimMovementResponse(
        m.getId(),
        m.getReference(),
        m.getClaimId(),
        m.getClaimNo(),
        m.getPolicyId(),
        m.getBusinessLine(),
        m.getLossDate(),
        m.getMovementDate(),
        m.getMovementType(),
        m.getCurrency(),
        m.getAmount(),
        m.getBaseAmount(),
        m.ceded(),
        m.getNetRetained(),
        m.getShares().stream().map(Share::from).toList());
  }

  /**
   * A reinsurer's share.
   *
   * @param layer layer
   * @param treatyId treaty
   * @param layerNo excess of loss layer
   * @param placementId facultative placement
   * @param reinsurerCode reinsurer
   * @param sharePct share %
   * @param baseAmount base currency amount
   */
  public record Share(
      RiLayer layer,
      Long treatyId,
      Integer layerNo,
      Long placementId,
      String reinsurerCode,
      BigDecimal sharePct,
      BigDecimal baseAmount) {

    static Share from(RiClaimShare s) {
      return new Share(
          s.getLayer(),
          s.getTreatyId(),
          s.getLayerNo(),
          s.getPlacementId(),
          s.getPartyCode(),
          s.getSharePct(),
          s.getBaseAmount());
    }
  }
}
