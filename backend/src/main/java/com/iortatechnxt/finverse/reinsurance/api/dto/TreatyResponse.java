package com.iortatechnxt.finverse.reinsurance.api.dto;

import com.iortatechnxt.finverse.common.domain.RecordStatus;
import com.iortatechnxt.finverse.reinsurance.domain.Treaty;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyLayer;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyParticipant;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Treaty with participants and layers.
 *
 * @param id id
 * @param companyId company
 * @param code code
 * @param name name
 * @param treatyType type
 * @param businessLine line of business
 * @param uwYear underwriting year
 * @param periodFrom period start
 * @param periodTo period end
 * @param currency currency
 * @param quotaSharePct quota share %
 * @param treatyLimit quota share limit per risk
 * @param retentionLimit retention line
 * @param lines surplus lines
 * @param levyPct levy %
 * @param reserveInterestPct interest on reserves %
 * @param lossReservePct loss reserve withheld %
 * @param statementFrequency statement frequency
 * @param brokerCode broker code
 * @param brokerName broker name
 * @param participants participants
 * @param layers layers
 * @param recordStatus maker-checker status
 * @param createdBy maker
 * @param updatedBy last modifier
 * @param authorizedBy checker
 * @param authorizedAt authorization time
 */
public record TreatyResponse(
    Long id,
    Long companyId,
    String code,
    String name,
    TreatyType treatyType,
    String businessLine,
    int uwYear,
    LocalDate periodFrom,
    LocalDate periodTo,
    String currency,
    BigDecimal quotaSharePct,
    BigDecimal treatyLimit,
    BigDecimal retentionLimit,
    Integer lines,
    BigDecimal levyPct,
    BigDecimal reserveInterestPct,
    BigDecimal lossReservePct,
    String statementFrequency,
    String brokerCode,
    String brokerName,
    List<Participant> participants,
    List<Layer> layers,
    RecordStatus recordStatus,
    String createdBy,
    String updatedBy,
    String authorizedBy,
    Instant authorizedAt) {

  /** Canonical constructor copying the lists. */
  public TreatyResponse {
    participants = List.copyOf(participants);
    layers = List.copyOf(layers);
  }

  /**
   * Maps a treaty.
   *
   * @param t treaty
   * @return response
   */
  public static TreatyResponse from(Treaty t) {
    return new TreatyResponse(
        t.getId(),
        t.getCompanyId(),
        t.getCode(),
        t.getName(),
        t.getTreatyType(),
        t.getBusinessLine(),
        t.getUwYear(),
        t.getPeriodFrom(),
        t.getPeriodTo(),
        t.getCurrency(),
        t.getQuotaSharePct(),
        t.getTreatyLimit(),
        t.getRetentionLimit(),
        t.getLines(),
        t.getLevyPct(),
        t.getReserveInterestPct(),
        t.getLossReservePct(),
        t.getStatementFrequency(),
        t.getBroker() == null ? null : t.getBroker().getCode(),
        t.getBroker() == null ? null : t.getBroker().getName(),
        t.getParticipants().stream().map(Participant::from).toList(),
        t.getLayers().stream().map(Layer::from).toList(),
        t.getRecordStatus(),
        t.getCreatedBy(),
        t.getUpdatedBy(),
        t.getAuthorizedBy(),
        t.getAuthorizedAt());
  }

  /**
   * Participant.
   *
   * @param lineNo line
   * @param reinsurerCode reinsurer code
   * @param reinsurerName reinsurer name
   * @param sharePct share %
   * @param commissionPct commission %
   * @param profitCommissionPct profit commission %
   * @param premiumReservePct premium reserve %
   */
  public record Participant(
      int lineNo,
      String reinsurerCode,
      String reinsurerName,
      BigDecimal sharePct,
      BigDecimal commissionPct,
      BigDecimal profitCommissionPct,
      BigDecimal premiumReservePct) {

    static Participant from(TreatyParticipant p) {
      return new Participant(
          p.getLineNo(),
          p.getParty().getCode(),
          p.getParty().getName(),
          p.getSharePct(),
          p.getCommissionPct(),
          p.getProfitCommissionPct(),
          p.getPremiumReservePct());
    }
  }

  /**
   * Excess of loss layer.
   *
   * @param layerNo layer number
   * @param priority priority
   * @param limit limit
   * @param minDepositPremium minimum and deposit premium
   * @param reinstatements reinstatements
   */
  public record Layer(
      int layerNo,
      BigDecimal priority,
      BigDecimal limit,
      BigDecimal minDepositPremium,
      int reinstatements) {

    static Layer from(TreatyLayer l) {
      return new Layer(
          l.getLayerNo(),
          l.getPriority(),
          l.getLayerLimit(),
          l.getMinDepositPremium(),
          l.getReinstatements());
    }
  }
}
