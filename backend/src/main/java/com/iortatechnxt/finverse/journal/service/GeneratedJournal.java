package com.iortatechnxt.finverse.journal.service;

import java.time.LocalDate;

/**
 * A journal generated from a recurring template occurrence.
 *
 * @param templateId template
 * @param templateName template name
 * @param occurrenceDate occurrence (value) date
 * @param batchId generated journal
 * @param batchNo generated journal number
 * @param reversalBatchNo generated reversing journal number (null when none)
 * @param submitted true when the journal was submitted for approval
 * @param note remark, e.g. why automatic submission failed
 */
public record GeneratedJournal(
    Long templateId,
    String templateName,
    LocalDate occurrenceDate,
    Long batchId,
    String batchNo,
    String reversalBatchNo,
    boolean submitted,
    String note) {

  /**
   * Copy with the submission outcome.
   *
   * @param isSubmitted submission succeeded
   * @param remark remark
   * @return updated copy
   */
  public GeneratedJournal withSubmission(boolean isSubmitted, String remark) {
    return new GeneratedJournal(
        templateId,
        templateName,
        occurrenceDate,
        batchId,
        batchNo,
        reversalBatchNo,
        isSubmitted,
        remark);
  }
}
