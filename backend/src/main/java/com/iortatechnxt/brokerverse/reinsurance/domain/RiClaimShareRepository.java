package com.iortatechnxt.brokerverse.reinsurance.domain;

import com.iortatechnxt.brokerverse.insurance.ClaimMovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Aggregate queries on the reinsurers' shares of claim movements. */
public interface RiClaimShareRepository extends JpaRepository<RiClaimShare, Long> {

  /**
   * A participant's share of the claim movements of one type in a period.
   *
   * @param treatyId treaty
   * @param partyId participant
   * @param type movement type
   * @param from first movement date
   * @param to last movement date
   * @return signed base amount, null when none
   */
  @Query(
      "select sum(s.baseAmount) from RiClaimShare s where s.treatyId = :treatyId"
          + " and s.partyId = :partyId and s.movement.movementType = :type"
          + " and s.movement.movementDate between :from and :to")
  BigDecimal total(
      @Param("treatyId") Long treatyId,
      @Param("partyId") Long partyId,
      @Param("type") ClaimMovementType type,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  /**
   * Accounting references of the recoveries of a treaty participant in a period.
   *
   * @param treatyId treaty
   * @param partyId participant
   * @param from first movement date
   * @param to last movement date
   * @return source references of the open items
   */
  @Query(
      "select distinct s.postingRef from RiClaimShare s where s.treatyId = :treatyId"
          + " and s.partyId = :partyId and s.postingRef is not null"
          + " and s.movement.movementType <> com.iortatechnxt.brokerverse.insurance.ClaimMovementType"
          + ".RESERVE_CHANGE and s.movement.movementDate between :from and :to")
  List<String> recoveryRefs(
      @Param("treatyId") Long treatyId,
      @Param("partyId") Long partyId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  /**
   * Excess of loss recoveries of a layer so far, all claims (aggregate limit control).
   *
   * @param treatyId treaty
   * @param layerNo layer
   * @return base amount, null when none
   */
  @Query(
      "select sum(s.baseAmount) from RiClaimShare s where s.treatyId = :treatyId"
          + " and s.layerNo = :layerNo")
  BigDecimal layerTotal(@Param("treatyId") Long treatyId, @Param("layerNo") Integer layerNo);

  /**
   * Excess of loss recoveries of a layer for one claim so far.
   *
   * @param claimId claim
   * @param treatyId treaty
   * @param layerNo layer
   * @return base amount, null when none
   */
  @Query(
      "select sum(s.baseAmount) from RiClaimShare s where s.movement.claimId = :claimId"
          + " and s.treatyId = :treatyId and s.layerNo = :layerNo")
  BigDecimal claimLayerTotal(
      @Param("claimId") Long claimId,
      @Param("treatyId") Long treatyId,
      @Param("layerNo") Integer layerNo);

  /**
   * Reinsurers' share of the outstanding reserve of each claim at a date.
   *
   * @param companyId company
   * @param asOf last movement date
   * @return claim id (first element) and base amount (second element)
   */
  @Query(
      "select new com.iortatechnxt.brokerverse.reinsurance.domain.ClaimAmount(m.claimId,"
          + " sum(s.baseAmount)) from RiClaimShare s join s.movement m"
          + " where m.companyId = :companyId and m.movementDate <= :asOf"
          + " and m.movementType = com.iortatechnxt.brokerverse.insurance.ClaimMovementType"
          + ".RESERVE_CHANGE group by m.claimId")
  List<ClaimAmount> reserveShares(
      @Param("companyId") Long companyId, @Param("asOf") LocalDate asOf);

  /**
   * A participant's share of outstanding reserves at a date.
   *
   * @param treatyId treaty
   * @param partyId participant
   * @param asOf last movement date
   * @return base amount, null when none
   */
  @Query(
      "select sum(s.baseAmount) from RiClaimShare s where s.treatyId = :treatyId"
          + " and s.partyId = :partyId and s.movement.movementDate <= :asOf"
          + " and s.movement.movementType = com.iortatechnxt.brokerverse.insurance.ClaimMovementType"
          + ".RESERVE_CHANGE")
  BigDecimal reserveShare(
      @Param("treatyId") Long treatyId,
      @Param("partyId") Long partyId,
      @Param("asOf") LocalDate asOf);
}
