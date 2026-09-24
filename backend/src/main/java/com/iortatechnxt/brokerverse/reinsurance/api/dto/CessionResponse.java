package com.iortatechnxt.brokerverse.reinsurance.api.dto;

import com.iortatechnxt.brokerverse.reinsurance.domain.Cession;
import com.iortatechnxt.brokerverse.reinsurance.domain.CessionBasis;
import com.iortatechnxt.brokerverse.reinsurance.domain.CessionLine;
import com.iortatechnxt.brokerverse.reinsurance.domain.RiLayer;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Cession of one premium transaction with its allocation lines (policy currency).
 *
 * @param id id
 * @param cessionNo cession number
 * @param policyId policy
 * @param policyNo policy number
 * @param endorsementNo endorsement number
 * @param documentNo document number
 * @param kind NEW or endorsement type
 * @param basis allocation basis
 * @param businessLine line of business
 * @param productCode product
 * @param uwYear underwriting year
 * @param treatyYear treaty programme year
 * @param riDate RI accounting date
 * @param currency policy currency
 * @param exchangeRate rate to base currency
 * @param sharePct company share %
 * @param ourSi company sum insured
 * @param ourPremium company net premium
 * @param treatyPremium quota share + surplus premium
 * @param facPremium facultative premium
 * @param retention net retention premium
 * @param lines allocation lines
 * @param createdBy user who ran the allocation
 * @param createdAt allocation time
 */
public record CessionResponse(
    Long id,
    String cessionNo,
    Long policyId,
    String policyNo,
    int endorsementNo,
    String documentNo,
    String kind,
    CessionBasis basis,
    String businessLine,
    String productCode,
    int uwYear,
    int treatyYear,
    LocalDate riDate,
    String currency,
    BigDecimal exchangeRate,
    BigDecimal sharePct,
    BigDecimal ourSi,
    BigDecimal ourPremium,
    BigDecimal treatyPremium,
    BigDecimal facPremium,
    BigDecimal retention,
    List<Line> lines,
    String createdBy,
    Instant createdAt) {

  /** Canonical constructor copying the lines. */
  public CessionResponse {
    lines = List.copyOf(lines);
  }

  /**
   * Maps a cession (lines loaded).
   *
   * @param c cession
   * @return response
   */
  public static CessionResponse from(Cession c) {
    BigDecimal treaty = c.premiumOf(RiLayer.QUOTA_SHARE).add(c.premiumOf(RiLayer.SURPLUS));
    BigDecimal fac = c.premiumOf(RiLayer.FAC);
    return new CessionResponse(
        c.getId(),
        c.getCessionNo(),
        c.getPolicyId(),
        c.getPolicyNo(),
        c.getEndorsementNo(),
        c.getDocumentNo(),
        c.getKind(),
        c.getBasis(),
        c.getBusinessLine(),
        c.getProductCode(),
        c.getUwYear(),
        c.getTreatyYear(),
        c.getRiDate(),
        c.getCurrency(),
        c.getExchangeRate(),
        c.getSharePct(),
        c.getOurSi(),
        c.getOurPremium(),
        treaty,
        fac,
        c.getOurPremium().subtract(treaty).subtract(fac),
        c.getLines().stream().map(Line::from).toList(),
        c.getCreatedBy(),
        c.getCreatedAt());
  }

  /**
   * One allocation line.
   *
   * @param riskLineNo risk line
   * @param riskDescription risk
   * @param riskSi company sum insured of the risk
   * @param layer layer
   * @param treatyId treaty
   * @param placementId facultative placement
   * @param reinsurerCode reinsurer, null for the retention and unplaced facultative
   * @param sharePct share of the risk sum insured %
   * @param sumInsured sum insured
   * @param premium premium
   * @param commissionPct commission %
   * @param commission commission
   * @param postingRef accounting reference
   */
  public record Line(
      int riskLineNo,
      String riskDescription,
      BigDecimal riskSi,
      RiLayer layer,
      Long treatyId,
      Long placementId,
      String reinsurerCode,
      BigDecimal sharePct,
      BigDecimal sumInsured,
      BigDecimal premium,
      BigDecimal commissionPct,
      BigDecimal commission,
      String postingRef) {

    static Line from(CessionLine l) {
      return new Line(
          l.getRiskLineNo(),
          l.getRiskDescription(),
          l.getRiskSi(),
          l.getLayer(),
          l.getTreatyId(),
          l.getPlacementId(),
          l.getPartyCode(),
          l.getSharePct(),
          l.getSumInsured(),
          l.getPremium(),
          l.getCommissionPct(),
          l.getCommission(),
          l.getPostingRef());
    }
  }
}
