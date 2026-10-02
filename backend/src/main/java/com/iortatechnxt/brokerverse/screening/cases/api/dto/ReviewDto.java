package com.iortatechnxt.brokerverse.screening.cases.api.dto;

import com.iortatechnxt.brokerverse.screening.cases.service.CaseReviewService.ReviewForm;
import com.iortatechnxt.brokerverse.screening.config.service.ReviewTemplate;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The review of a case: its template version, fields and answers (FR-SS-050 Review tab).
 *
 * @param templateVersionId the TEMPLATE version
 * @param templateType the template type
 * @param name the template name
 * @param status DRAFT or SUBMITTED
 * @param submittedBy submitter
 * @param submittedAt submission time
 * @param fields the fields in order
 * @param values raw values by field code
 */
public record ReviewDto(
    Long templateVersionId,
    String templateType,
    String name,
    String status,
    String submittedBy,
    Instant submittedAt,
    List<FieldDto> fields,
    Map<String, String> values) {

  /**
   * Maps a review form.
   *
   * @param form the review
   * @return the DTO
   */
  public static ReviewDto from(ReviewForm form) {
    return new ReviewDto(
        form.review().getTemplateVersionId(),
        form.review().getTemplateType(),
        form.template().name(),
        form.review().getStatus().name(),
        form.review().getSubmittedBy(),
        form.review().getSubmittedAt(),
        form.template().fields().stream().map(FieldDto::from).toList(),
        Map.copyOf(form.values()));
  }

  /**
   * One template field.
   *
   * @param code code
   * @param section section
   * @param label label
   * @param dataType data type
   * @param lovType list of values
   * @param mandatory mandatory
   * @param help help text
   */
  public record FieldDto(
      String code,
      String section,
      String label,
      String dataType,
      String lovType,
      boolean mandatory,
      String help) {

    /**
     * Maps a field.
     *
     * @param f the field
     * @return the DTO
     */
    public static FieldDto from(ReviewTemplate.Field f) {
      return new FieldDto(
          f.code(),
          f.section(),
          f.label(),
          f.dataType().name(),
          f.lovType(),
          f.mandatory(),
          f.help());
    }
  }
}
