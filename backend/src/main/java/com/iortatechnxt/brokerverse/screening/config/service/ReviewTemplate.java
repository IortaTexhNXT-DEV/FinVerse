package com.iortatechnxt.brokerverse.screening.config.service;

import com.iortatechnxt.brokerverse.screening.config.domain.FieldDataType;
import com.iortatechnxt.brokerverse.screening.config.domain.TemplateType;
import java.util.Comparator;
import java.util.List;

/**
 * A review or STR template version (SNSRP-104, 105, 501; FR-SS-016, 017). A review or STR stores
 * {@code version().id()} and keeps it when a newer version becomes active.
 *
 * @param version the TEMPLATE configuration version (scope = template type)
 * @param templateType KYC_REVIEW, TRANSACTION_REVIEW, EDD or STR
 * @param name display name
 * @param fields the fields, sorted by section order then field order
 */
public record ReviewTemplate(
    ConfigVersionRef version, TemplateType templateType, String name, List<Field> fields) {

  /** Sorted copy. */
  public ReviewTemplate {
    fields = fields.stream().sorted(Comparator.comparingInt(Field::order)).toList();
  }

  /**
   * One template field.
   *
   * @param id the field id
   * @param section the section heading
   * @param code unique code in the template (letters, digits, underscore)
   * @param label the label shown to the reviewer
   * @param dataType the data type
   * @param lovType the list of values for {@link FieldDataType#LOV}, otherwise {@code null}
   * @param mandatory whether the review cannot be submitted without it
   * @param help help text, may be {@code null}
   * @param order display order in the template
   * @param prefillSource STR only: the case data that prefills the field, may be {@code null}
   */
  public record Field(
      Long id,
      String section,
      String code,
      String label,
      FieldDataType dataType,
      String lovType,
      boolean mandatory,
      String help,
      int order,
      String prefillSource) {}
}
