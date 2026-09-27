package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.Clause;
import com.iortatechnxt.brokerverse.catalog.domain.Clause.ClauseDetails;
import com.iortatechnxt.brokerverse.catalog.domain.Coverage;
import com.iortatechnxt.brokerverse.catalog.domain.Coverage.CoverageDetails;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Request and response bodies of the coverage master and clause library (PMADD01/02). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class CoverageDtos {

  private static final String CODE_PATTERN = "[A-Z0-9_]+";
  private static final String CODE_MESSAGE = "use A-Z, 0-9 and _";

  private CoverageDtos() {}

  /**
   * New or changed coverage.
   *
   * @param lineCode product line (ignored on update)
   * @param code code (ignored on update)
   * @param name name
   * @param kind kind (LOV COVERAGE_KIND)
   * @param basic basic cover
   * @param sortOrder display order
   */
  public record CoverageRequest(
      @NotBlank @Size(max = 30) String lineCode,
      @NotBlank @Size(max = 30) @Pattern(regexp = CODE_PATTERN, message = CODE_MESSAGE) String code,
      @NotBlank @Size(max = 150) String name,
      @NotBlank @Size(max = 20) String kind,
      boolean basic,
      @PositiveOrZero int sortOrder) {

    /**
     * Maintainable attributes.
     *
     * @return details
     */
    public CoverageDetails details() {
      return new CoverageDetails(name.strip(), kind, basic, sortOrder);
    }
  }

  /**
   * A coverage.
   *
   * @param id id
   * @param lineCode product line
   * @param code code
   * @param name name
   * @param kind kind
   * @param basic basic cover
   * @param sortOrder order
   * @param recordStatus maker-checker status
   * @param maker last maintainer
   * @param authorizedBy checker
   */
  public record CoverageResponse(
      Long id,
      String lineCode,
      String code,
      String name,
      String kind,
      boolean basic,
      int sortOrder,
      RecordStatus recordStatus,
      String maker,
      String authorizedBy) {

    /**
     * Maps an entity.
     *
     * @param e entity
     * @return response
     */
    public static CoverageResponse from(Coverage e) {
      return new CoverageResponse(
          e.getId(),
          e.getLineCode(),
          e.getCode(),
          e.getName(),
          e.getKind(),
          e.isBasic(),
          e.getSortOrder(),
          e.getRecordStatus(),
          e.getMaker(),
          e.getAuthorizedBy());
    }
  }

  /**
   * New or changed clause.
   *
   * @param code code (ignored on update)
   * @param kind kind (LOV CLAUSE_KIND)
   * @param lineCode product line, empty for every line
   * @param title title
   * @param wording wording
   * @param effectiveFrom first valid date
   * @param effectiveTo last valid date, empty when open ended
   */
  public record ClauseRequest(
      @NotBlank @Size(max = 30) @Pattern(regexp = CODE_PATTERN, message = CODE_MESSAGE) String code,
      @NotBlank @Size(max = 20) String kind,
      @Size(max = 30) String lineCode,
      @NotBlank @Size(max = 200) String title,
      @NotBlank @Size(max = 20000) String wording,
      @NotNull LocalDate effectiveFrom,
      LocalDate effectiveTo) {

    /**
     * Maintainable attributes.
     *
     * @return details
     */
    public ClauseDetails details() {
      return new ClauseDetails(
          kind,
          lineCode == null || lineCode.isBlank() ? null : lineCode,
          title.strip(),
          wording.strip(),
          effectiveFrom,
          effectiveTo);
    }
  }

  /**
   * A clause.
   *
   * @param id id
   * @param code code
   * @param kind kind
   * @param lineCode line, null for every line
   * @param title title
   * @param wording wording
   * @param effectiveFrom first valid date
   * @param effectiveTo last valid date
   * @param recordStatus maker-checker status
   * @param maker last maintainer
   * @param authorizedBy checker
   */
  public record ClauseResponse(
      Long id,
      String code,
      String kind,
      String lineCode,
      String title,
      String wording,
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
    public static ClauseResponse from(Clause e) {
      return new ClauseResponse(
          e.getId(),
          e.getCode(),
          e.getKind(),
          e.getLineCode(),
          e.getTitle(),
          e.getWording(),
          e.getEffectiveFrom(),
          e.getEffectiveTo(),
          e.getRecordStatus(),
          e.getMaker(),
          e.getAuthorizedBy());
    }
  }
}
