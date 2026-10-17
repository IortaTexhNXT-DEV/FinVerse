package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.InsurerParticipation;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerParticipation.ParticipationDetails;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Requests and responses of the Co-insurance / Risk Participation tab of the insurer page. */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of the DTOs
public final class CoinsuranceDtos {

  private CoinsuranceDtos() {}

  /**
   * A participating insurer.
   *
   * @param participantCode party code of the participating insurer
   * @param sharePct share in percent
   * @param lead whether it leads
   * @param effectiveFrom first day
   * @param effectiveTo last day, null when open
   */
  public record ParticipationRequest(
      @NotBlank @Size(max = 30) String participantCode,
      @NotNull @DecimalMin(value = "0.0001") @DecimalMax(value = "100") BigDecimal sharePct,
      boolean lead,
      @NotNull LocalDate effectiveFrom,
      LocalDate effectiveTo) {

    /**
     * The details.
     *
     * @return details
     */
    public ParticipationDetails details() {
      return new ParticipationDetails(
          participantCode.strip(), sharePct, lead, effectiveFrom, effectiveTo);
    }
  }

  /**
   * A participation.
   *
   * @param id id
   * @param insurerId insurer profile of the arrangement
   * @param participantCode participating insurer
   * @param sharePct share in percent
   * @param lead whether it leads
   * @param effectiveFrom first day
   * @param effectiveTo last day
   * @param recordStatus maker-checker status
   * @param maker last maintainer
   * @param authorizedBy checker
   */
  public record ParticipationResponse(
      Long id,
      Long insurerId,
      String participantCode,
      BigDecimal sharePct,
      boolean lead,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      RecordStatus recordStatus,
      String maker,
      String authorizedBy) {

    /**
     * Maps an entity.
     *
     * @param e entity
     * @return response
     */
    public static ParticipationResponse from(InsurerParticipation e) {
      return new ParticipationResponse(
          e.getId(),
          e.getInsurerId(),
          e.getParticipantCode(),
          e.getSharePct(),
          e.isLead(),
          e.getEffectiveFrom(),
          e.getEffectiveTo(),
          e.getRecordStatus(),
          e.getMaker(),
          e.getAuthorizedBy());
    }
  }

  /**
   * The arrangement of an insurer.
   *
   * @param participations participations, latest period first
   * @param totalToday total of the authorized shares effective today
   * @param complete whether they total 100 % today (used as the default split)
   */
  public record ArrangementResponse(
      List<ParticipationResponse> participations, BigDecimal totalToday, boolean complete) {

    /** Defensive copy. */
    public ArrangementResponse {
      participations = List.copyOf(participations);
    }
  }
}
