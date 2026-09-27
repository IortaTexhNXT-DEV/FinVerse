package com.iortatechnxt.brokerverse.collections.setup.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.SalesUnit;
import com.iortatechnxt.brokerverse.collections.common.domain.LovAttribute;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.system.domain.SystemParameter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Requests and responses of Collections Setup (BRCLXN.005-007, 011, 016/017, 037). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class SetupDtos {

  private SetupDtos() {}

  /**
   * Collections Setup.
   *
   * @param parameters Collections parameters
   * @param dispositions PR and unapplied-payment disposition values with their attributes
   * @param units sales units with their Unit Head
   */
  public record SetupResponse(
      List<ParameterRow> parameters, List<DispositionRow> dispositions, List<UnitRow> units) {

    /** Defensive copies. */
    public SetupResponse {
      parameters = List.copyOf(parameters);
      dispositions = List.copyOf(dispositions);
      units = List.copyOf(units);
    }
  }

  /**
   * A parameter.
   *
   * @param key key
   * @param value value
   * @param valueType type
   * @param description description
   * @param minValue lowest value
   * @param maxValue highest value
   * @param updatedBy last changed by
   */
  public record ParameterRow(
      String key,
      String value,
      String valueType,
      String description,
      Integer minValue,
      Integer maxValue,
      String updatedBy) {

    /**
     * Maps a parameter.
     *
     * @param p parameter
     * @return row
     */
    public static ParameterRow from(SystemParameter p) {
      return new ParameterRow(
          p.getKey(),
          p.getValue(),
          p.getValueType().name(),
          p.getDescription(),
          p.getMinValue(),
          p.getMaxValue(),
          p.getUpdatedBy() == null ? p.getCreatedBy() : p.getUpdatedBy());
    }
  }

  /**
   * A disposition value with its attributes.
   *
   * @param typeCode LOV type
   * @param code code
   * @param label label
   * @param effectiveTo end of validity, null when open
   * @param attributes attribute values
   */
  public record DispositionRow(
      String typeCode,
      String code,
      String label,
      LocalDate effectiveTo,
      List<AttributeRow> attributes) {

    /** Defensive copy. */
    public DispositionRow {
      attributes = List.copyOf(attributes);
    }

    /**
     * Maps a value and its attributes.
     *
     * @param v LOV value
     * @param all attributes of its type
     * @return row
     */
    public static DispositionRow from(LovValue v, List<LovAttribute> all) {
      return new DispositionRow(
          v.getTypeCode(),
          v.getCode(),
          v.getLabel(),
          v.getEffectiveTo(),
          all.stream()
              .filter(
                  a -> a.getTypeCode().equals(v.getTypeCode()) && a.getCode().equals(v.getCode()))
              .map(a -> new AttributeRow(a.getAttribute(), a.getValue()))
              .toList());
    }
  }

  /**
   * An attribute value.
   *
   * @param attribute name
   * @param value value
   */
  public record AttributeRow(String attribute, String value) {}

  /**
   * A sales unit and its Unit Head.
   *
   * @param code code
   * @param name name
   * @param level REGION, DEPARTMENT or TEAM
   * @param parentCode parent unit
   * @param headUsername Unit Head
   */
  public record UnitRow(
      String code, String name, String level, String parentCode, String headUsername) {

    /**
     * Maps a unit.
     *
     * @param u unit
     * @return row
     */
    public static UnitRow from(SalesUnit u) {
      return new UnitRow(
          u.getCode(), u.getName(), u.getLevel().name(), u.getParentCode(), u.getHeadUsername());
    }
  }

  /**
   * A parameter change.
   *
   * @param value new value
   */
  public record ParameterRequest(@NotNull @Size(max = 500) String value) {}

  /**
   * An attribute change.
   *
   * @param typeCode CLX_PR_DISPOSITION or CLX_UPP_DISPOSITION
   * @param code value code
   * @param attribute attribute
   * @param value new value, blank to remove
   */
  public record AttributeRequest(
      @NotBlank @Size(max = 40) String typeCode,
      @NotBlank @Size(max = 40) String code,
      @NotBlank @Size(max = 40) String attribute,
      @Size(max = 200) String value) {}

  /**
   * A Unit Head change.
   *
   * @param username head, blank to clear
   */
  public record UnitHeadRequest(@Size(max = 50) String username) {}
}
