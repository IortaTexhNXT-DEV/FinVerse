package com.iortatechnxt.brokerverse.crm.domain;

import java.time.LocalDate;

/**
 * Content of a special instruction.
 *
 * @param type instruction type (list of values INSTRUCTION_TYPE)
 * @param text instruction text
 * @param effectiveFrom first day in force
 * @param effectiveTo last day in force, null when open-ended
 */
public record InstructionContent(
    String type, String text, LocalDate effectiveFrom, LocalDate effectiveTo) {

  /**
   * One-line description for the change history.
   *
   * @return description
   */
  public String describe() {
    return type
        + ": "
        + text
        + " ("
        + effectiveFrom
        + (effectiveTo == null ? " onwards" : " to " + effectiveTo)
        + ")";
  }
}
