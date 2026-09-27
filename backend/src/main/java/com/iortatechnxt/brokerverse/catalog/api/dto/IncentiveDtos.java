package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.IncentiveCriteria;
import com.iortatechnxt.brokerverse.catalog.domain.IncentiveCriteria.Details;
import com.iortatechnxt.brokerverse.catalog.domain.IncentiveScope;
import com.iortatechnxt.brokerverse.catalog.domain.IncentiveValueBasis;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Request and response bodies of the incentive criteria (PMADD07/08). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class IncentiveDtos {

  private IncentiveDtos() {}

  /**
   * New, changed or amended criterion.
   *
   * @param companyId company (ignored on update)
   * @param code code (ignored on update)
   * @param name name
   * @param incentiveType type (LOV INCENTIVE_TYPE)
   * @param valueBasis RATE, FIXED_AMOUNT or RULE
   * @param value value, empty for a RULE
   * @param ruleParams rule parameters as JSON
   * @param description description
   * @param products products-matrix entries
   * @param effectiveFrom first effective day
   * @param effectiveTo last effective day, empty when open ended
   */
  public record CriteriaRequest(
      @NotNull Long companyId,
      @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Z0-9_-]+", message = "use A-Z, 0-9, _ and -")
          String code,
      @NotBlank @Size(max = 150) String name,
      @NotBlank @Size(max = 30) String incentiveType,
      @NotNull IncentiveValueBasis valueBasis,
      @DecimalMin("0") BigDecimal value,
      @Size(max = 2000) String ruleParams,
      @Size(max = 1000) String description,
      @NotEmpty @Size(max = 200) List<@Valid ScopeDto> products,
      @NotNull LocalDate effectiveFrom,
      LocalDate effectiveTo) {

    /**
     * The criterion's attributes.
     *
     * @return details
     */
    public Details details() {
      return new Details(
          name.strip(),
          incentiveType,
          valueBasis,
          valueBasis == IncentiveValueBasis.RULE ? null : value,
          blankToNull(ruleParams),
          blankToNull(description),
          products.stream().map(ScopeDto::scope).toList(),
          effectiveFrom,
          effectiveTo);
    }
  }

  /**
   * A products-matrix entry.
   *
   * @param productCode risk code
   * @param coverTypeCode cover type, empty for any
   * @param marketSegment segment, empty for any
   * @param sourceChannel channel, empty for any
   * @param insurerCode insurer, empty for any
   */
  public record ScopeDto(
      @NotBlank @Size(max = 20) String productCode,
      @Size(max = 30) String coverTypeCode,
      @Size(max = 40) String marketSegment,
      @Size(max = 40) String sourceChannel,
      @Size(max = 30) String insurerCode) {

    IncentiveScope scope() {
      return new IncentiveScope(
          productCode,
          blankToNull(coverTypeCode),
          blankToNull(marketSegment),
          blankToNull(sourceChannel),
          blankToNull(insurerCode));
    }

    static ScopeDto from(IncentiveScope s) {
      return new ScopeDto(
          s.productCode(),
          s.coverTypeCode(),
          s.marketSegment(),
          s.sourceChannel(),
          s.insurerCode());
    }
  }

  /**
   * Deactivation of a criterion.
   *
   * @param lastDay last effective day, empty for today
   */
  public record DeactivateRequest(LocalDate lastDay) {}

  /**
   * A criterion row.
   *
   * @param id id
   * @param companyId company
   * @param code code
   * @param name name
   * @param incentiveType type
   * @param valueBasis value basis
   * @param value value
   * @param ruleParams rule parameters
   * @param description description
   * @param products products-matrix entries
   * @param effectiveFrom first effective day
   * @param effectiveTo last effective day
   * @param successorOf row it replaces
   * @param recordStatus maker-checker status
   * @param maker last maintainer
   * @param authorizedBy checker
   */
  public record CriteriaResponse(
      Long id,
      Long companyId,
      String code,
      String name,
      String incentiveType,
      IncentiveValueBasis valueBasis,
      BigDecimal value,
      String ruleParams,
      String description,
      List<ScopeDto> products,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      Long successorOf,
      RecordStatus recordStatus,
      String maker,
      String authorizedBy) {

    /**
     * Maps an entity.
     *
     * @param e entity
     * @return response
     */
    public static CriteriaResponse from(IncentiveCriteria e) {
      return new CriteriaResponse(
          e.getId(),
          e.getCompanyId(),
          e.getCode(),
          e.getName(),
          e.getIncentiveType(),
          e.getValueBasis(),
          e.getValue(),
          e.getRuleParams(),
          e.getDescription(),
          e.getScopes().stream().map(ScopeDto::from).toList(),
          e.getEffectiveFrom(),
          e.getEffectiveTo(),
          e.getSuccessorOf(),
          e.getRecordStatus(),
          e.getMaker(),
          e.getAuthorizedBy());
    }
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
