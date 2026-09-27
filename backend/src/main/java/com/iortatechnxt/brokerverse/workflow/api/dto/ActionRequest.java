package com.iortatechnxt.brokerverse.workflow.api.dto;

import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import jakarta.validation.constraints.Size;

/**
 * Reason and comment for an action.
 *
 * @param reasonCode reason code
 * @param comment comment
 */
public record ActionRequest(@Size(max = 40) String reasonCode, @Size(max = 1000) String comment) {

  /**
   * As a transition note.
   *
   * @return note
   */
  public TransitionNote note() {
    return new TransitionNote(
        reasonCode == null || reasonCode.isBlank() ? null : reasonCode,
        comment == null || comment.isBlank() ? null : comment.trim());
  }
}
