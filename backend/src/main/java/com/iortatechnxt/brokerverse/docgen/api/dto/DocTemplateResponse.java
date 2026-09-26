package com.iortatechnxt.brokerverse.docgen.api.dto;

import com.iortatechnxt.brokerverse.docgen.domain.DocTemplate;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A template version.
 *
 * @param id id
 * @param code template
 * @param versionNo version
 * @param title title
 * @param body text
 * @param effectiveFrom first date of use
 * @param active active
 * @param createdBy created by
 * @param createdAt created at
 */
public record DocTemplateResponse(
    Long id,
    String code,
    int versionNo,
    String title,
    String body,
    LocalDate effectiveFrom,
    boolean active,
    String createdBy,
    Instant createdAt) {

  /**
   * Maps a version.
   *
   * @param t version
   * @return response
   */
  public static DocTemplateResponse from(DocTemplate t) {
    return new DocTemplateResponse(
        t.getId(),
        t.getCode(),
        t.getVersionNo(),
        t.getTitle(),
        t.getBody(),
        t.getEffectiveFrom(),
        t.isActive(),
        t.getCreatedBy(),
        t.getCreatedAt());
  }
}
