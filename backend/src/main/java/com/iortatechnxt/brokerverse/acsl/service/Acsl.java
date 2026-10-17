package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;

/** Names and small helpers shared by the ACSL services. */
public final class Acsl {

  /** Module name on journals, open items, ledger movements, ports and the audit trail. */
  public static final String MODULE = "ACSL";

  /** Entity type of a case (work case, attachments, audit trail). */
  public static final String CASE_ENTITY = "AcslCase";

  /** Entity type of a correction entry. */
  public static final String CORRECTION_ENTITY = "AcslCorrection";

  /** Entity type of an SOA upload. */
  public static final String SOA_ENTITY = "AcslSoaUpload";

  /** Workflow of the cases (V890). */
  public static final String CASE_WORKFLOW = "ACSL_CASE";

  /** Workflow of the correction entries (V890). */
  public static final String CORRECTION_WORKFLOW = "ACSL_CORRECTION";

  /** Notification event of cases and corrections (V897). */
  public static final String STATUS_EVENT = "ACSL_CASE_STATUS";

  /** Error code of an action in the wrong stage. */
  public static final String WRONG_STAGE = "ACSL_WRONG_STAGE";

  /** Error code of a four-eyes violation. */
  public static final String FOUR_EYES = "ACSL_FOUR_EYES";

  private Acsl() {}

  /**
   * Route of a case page.
   *
   * @param id case
   * @return route
   */
  public static String caseLink(Long id) {
    return "/acsl/cases/" + id;
  }

  /**
   * Route of a correction page.
   *
   * @param id correction
   * @return route
   */
  public static String correctionLink(Long id) {
    return "/acsl/corrections/" + id;
  }

  /**
   * A workflow note from an optional comment.
   *
   * @param comment comment, may be blank
   * @return note
   */
  public static TransitionNote note(String comment) {
    return TransitionNote.comment(blankToNull(comment));
  }

  /**
   * Trims a text; blank becomes null.
   *
   * @param value text
   * @return trimmed text or null
   */
  public static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * Refuses an action when a record is not in the expected stage.
   *
   * @param reference record reference
   * @param actual current stage
   * @param expected expected stage
   * @param <E> stage type
   */
  public static <E extends Enum<E>> void requireStage(String reference, E actual, E expected) {
    if (actual != expected) {
      throw new BusinessRuleException(
          WRONG_STAGE, reference + " is " + actual + ", not " + expected);
    }
  }
}
