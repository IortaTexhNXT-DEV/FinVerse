package com.iortatechnxt.brokerverse.account.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Comment given with an account action.
 *
 * @param comment comment
 */
public record CommentRequest(@Size(max = 1000) String comment) {

  /**
   * The comment, null when blank.
   *
   * @return comment
   */
  public String text() {
    return comment == null || comment.isBlank() ? null : comment.strip();
  }
}
