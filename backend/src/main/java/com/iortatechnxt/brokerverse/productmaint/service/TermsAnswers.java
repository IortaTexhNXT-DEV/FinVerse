package com.iortatechnxt.brokerverse.productmaint.service;

import java.util.Map;

/** The names of the insurer responses of the comparative table: Approved, Not Covered, Others. */
public final class TermsAnswers {

  private static final Map<String, String> NAMES =
      Map.of("APPROVED", "Approved", "NOT_COVERED", "Not Covered", "OTHERS", "Others");

  private TermsAnswers() {}

  /**
   * The name of a response; Others shows the insurer's own wording.
   *
   * @param answer APPROVED, NOT_COVERED or OTHERS; null while the insurer has not responded
   * @param otherAnswer the insurer's own wording
   * @return name
   */
  public static String label(String answer, String otherAnswer) {
    if (answer == null) {
      return "Awaiting response";
    }
    if ("OTHERS".equals(answer) && otherAnswer != null && !otherAnswer.isBlank()) {
      return "Others: " + otherAnswer;
    }
    return NAMES.getOrDefault(answer, answer);
  }
}
