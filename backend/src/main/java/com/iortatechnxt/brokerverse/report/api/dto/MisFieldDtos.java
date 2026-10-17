package com.iortatechnxt.brokerverse.report.api.dto;

import com.iortatechnxt.brokerverse.report.core.MisDataElement;
import com.iortatechnxt.brokerverse.report.core.MisDataElement.MisEntity;
import com.iortatechnxt.brokerverse.report.domain.MisField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Requests and views of the MIS Fields screen (FR-CR-062). */
public interface MisFieldDtos {

  /**
   * A field to add or change.
   *
   * @param code unique code (kept on a change)
   * @param label business label
   * @param entity core entity (kept on a change)
   * @param source data element of the entity (kept on a change)
   * @param lovType list of values, may be null
   */
  record MisFieldRequest(
      @Size(max = 40) String code,
      @NotBlank @Size(max = 80) String label,
      @NotNull MisEntity entity,
      @NotNull MisDataElement source,
      @Size(max = 60) String lovType) {}

  /**
   * A field of the catalogue.
   *
   * @param id id
   * @param code code
   * @param label business label
   * @param entity core entity
   * @param source data element
   * @param columnKeys report column keys that carry the data element
   * @param lovType list of values
   * @param recordStatus PENDING_AUTHORIZATION, ACTIVE or INACTIVE
   * @param maker who last maintained it
   * @param authorizedBy checker
   * @param authorizedAt when it was authorized
   */
  record MisFieldView(
      Long id,
      String code,
      String label,
      MisEntity entity,
      MisDataElement source,
      List<String> columnKeys,
      String lovType,
      String recordStatus,
      String maker,
      String authorizedBy,
      Instant authorizedAt) {

    /**
     * Maps a field.
     *
     * @param f field
     * @return view
     */
    public static MisFieldView from(MisField f) {
      return new MisFieldView(
          f.getId(),
          f.getCode(),
          f.getLabel(),
          f.getEntity(),
          f.getSource(),
          f.getSource().columnKeys(),
          f.getLovType(),
          f.getRecordStatus().name(),
          f.getMaker(),
          f.getAuthorizedBy(),
          f.getAuthorizedAt());
    }
  }
}
