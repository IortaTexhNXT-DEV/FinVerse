package com.iortatechnxt.brokerverse.docgen.api.dto;

import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService.TemplateDraft;
import java.util.List;

/**
 * The draft of a new template version read from Word (client requirement 16).
 *
 * @param title title (first paragraph)
 * @param body text with placeholders
 * @param missingPlaceholders placeholders of the latest version the draft no longer has
 * @param addedPlaceholders placeholders the latest version does not have
 */
public record TemplateDraftResponse(
    String title, String body, List<String> missingPlaceholders, List<String> addedPlaceholders) {

  /**
   * Maps a draft.
   *
   * @param d draft
   * @return response
   */
  public static TemplateDraftResponse from(TemplateDraft d) {
    return new TemplateDraftResponse(
        d.title(), d.body(), d.missingPlaceholders(), d.addedPlaceholders());
  }
}
