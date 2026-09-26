package com.iortatechnxt.brokerverse.reinsurance.api.dto;

import com.iortatechnxt.brokerverse.reinsurance.domain.Cession;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacParticipant;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacPlacement;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Facultative placement with its participants (policy currency).
 *
 * @param id id
 * @param placementNo placement number
 * @param cessionNo cession that identified the requirement
 * @param policyId policy
 * @param policyNo policy number
 * @param endorsementNo endorsement number of the cession
 * @param businessLine line of business
 * @param currency policy currency
 * @param riDate RI accounting date of the cession
 * @param riskLineNo risk line
 * @param riskDescription risk
 * @param riskSi company sum insured of the risk
 * @param riskPremium company premium of the risk
 * @param facPct facultative share of the risk %
 * @param facSi facultative sum insured required
 * @param facPremium facultative premium
 * @param placedSi sum insured placed
 * @param placedPremium premium placed
 * @param placementPct placed SI / FAC SI x 100
 * @param commission participants' commission
 * @param status status
 * @param submittedBy maker
 * @param submittedAt submission time
 * @param placedBy checker
 * @param placedOn placement date
 * @param closedOn closing date
 * @param remarks remarks
 * @param participants participants
 */
public record FacPlacementResponse(
    Long id,
    String placementNo,
    String cessionNo,
    Long policyId,
    String policyNo,
    int endorsementNo,
    String businessLine,
    String currency,
    LocalDate riDate,
    int riskLineNo,
    String riskDescription,
    BigDecimal riskSi,
    BigDecimal riskPremium,
    BigDecimal facPct,
    BigDecimal facSi,
    BigDecimal facPremium,
    BigDecimal placedSi,
    BigDecimal placedPremium,
    BigDecimal placementPct,
    BigDecimal commission,
    FacStatus status,
    String submittedBy,
    Instant submittedAt,
    String placedBy,
    LocalDate placedOn,
    LocalDate closedOn,
    String remarks,
    List<Participant> participants) {

  /** Canonical constructor copying the participants. */
  public FacPlacementResponse {
    participants = List.copyOf(participants);
  }

  /**
   * Maps a placement.
   *
   * @param f placement
   * @return response
   */
  public static FacPlacementResponse from(FacPlacement f) {
    Cession c = f.getCession();
    return new FacPlacementResponse(
        f.getId(),
        f.getPlacementNo(),
        c.getCessionNo(),
        c.getPolicyId(),
        c.getPolicyNo(),
        c.getEndorsementNo(),
        c.getBusinessLine(),
        c.getCurrency(),
        c.getRiDate(),
        f.getRiskLineNo(),
        f.getRiskDescription(),
        f.getRiskSi(),
        f.getRiskPremium(),
        f.getFacPct(),
        f.getFacSi(),
        f.getFacPremium(),
        f.getPlacedSi(),
        f.getPlacedPremium(),
        f.placementPct(),
        f.commission(),
        f.getStatus(),
        f.getSubmittedBy(),
        f.getSubmittedAt(),
        f.getPlacedBy(),
        f.getPlacedOn(),
        f.getClosedOn(),
        f.getRemarks(),
        f.getParticipants().stream().map(Participant::from).toList());
  }

  /**
   * A participant's line.
   *
   * @param lineNo line
   * @param reinsurerCode reinsurer code
   * @param reinsurerName reinsurer name
   * @param sharePct share of the facultative requirement %
   * @param commissionPct commission %
   * @param sumInsured accepted sum insured
   * @param premium accepted premium
   * @param commission commission
   */
  public record Participant(
      int lineNo,
      String reinsurerCode,
      String reinsurerName,
      BigDecimal sharePct,
      BigDecimal commissionPct,
      BigDecimal sumInsured,
      BigDecimal premium,
      BigDecimal commission) {

    static Participant from(FacParticipant p) {
      return new Participant(
          p.getLineNo(),
          p.getParty().getCode(),
          p.getParty().getName(),
          p.getSharePct(),
          p.getCommissionPct(),
          p.getSumInsured(),
          p.getPremium(),
          p.getCommission());
    }
  }
}
