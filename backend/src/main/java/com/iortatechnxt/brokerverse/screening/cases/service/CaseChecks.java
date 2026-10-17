package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.FieldValidationException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent.EventFacts;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;

/**
 * Checks shared by the case actions (FR-SS-051, 061-064): mandatory texts with the FRS messages,
 * text limits, the refusal of a failed validation (logged on the timeline first) and the case
 * lookup.
 */
final class CaseChecks {

  /** Longest recommendation, rationale or remarks (FR-SS-051, 064). */
  static final int MAX_TEXT = 4000;

  /** Longest comment kept in the workflow history ({@code wf_case_history.comment}). */
  static final int MAX_COMMENT = 1000;

  private CaseChecks() {}

  /**
   * Refuses a blank text.
   *
   * @param value the text
   * @param code the error code
   * @param message the FRS message
   * @return the stripped text
   */
  static String require(String value, String code, String message) {
    if (value == null || value.isBlank()) {
      throw new BusinessRuleException(code, message);
    }
    if (value.length() > MAX_TEXT) {
      throw new BusinessRuleException(
          "SCR_TEXT_TOO_LONG", "The text is limited to " + MAX_TEXT + " characters");
    }
    return value.strip();
  }

  /**
   * A workflow note with the text cut to the history column.
   *
   * @param reasonCode reason, may be null
   * @param text comment, may be null
   * @return note
   */
  static TransitionNote note(String reasonCode, String text) {
    String comment =
        text == null || text.length() <= MAX_COMMENT ? text : text.substring(0, MAX_COMMENT);
    return new TransitionNote(reasonCode, comment);
  }

  /**
   * Refuses a disposition that is not a value of the stage (SNSRP-107).
   *
   * @param validator validator
   * @param stage the stage
   * @param disposition the disposition
   */
  static void requireAllowed(CaseValidator validator, CaseStage stage, String disposition) {
    if (!validator.allowed(stage, disposition)) {
      throw new BusinessRuleException(
          "SCR_DISPOSITION_INVALID",
          "Disposition " + disposition + " is not a disposition of stage " + stage);
    }
  }

  /**
   * Logs a failed validation on the timeline (in its own transaction) and refuses the action.
   *
   * @param c the case
   * @param timeline timeline
   * @param stage the stage the case stays in
   * @param v the validation
   */
  static void refuse(
      ScreeningCase c, CaseTimeline timeline, CaseStage stage, CaseValidator.Validation v) {
    timeline.recordAside(
        c.getId(),
        CaseEventType.VALIDATION_FAILED,
        EventFacts.move(stage.name(), stage.name(), null, v.summary()));
    String message = String.join("; ", v.blocking());
    if (v.fields().isEmpty()) {
      throw new BusinessRuleException("SCR_VALIDATION_FAILED", message);
    }
    throw new FieldValidationException("SCR_VALIDATION_FAILED", message, v.fields());
  }

  /**
   * A case by id.
   *
   * @param cases repository
   * @param id id
   * @return the case
   */
  static ScreeningCase get(ScreeningCaseRepository cases, Long id) {
    return cases
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(CaseCodes.ENTITY, id));
  }
}
