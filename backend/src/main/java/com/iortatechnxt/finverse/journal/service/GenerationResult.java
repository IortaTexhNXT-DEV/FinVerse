package com.iortatechnxt.finverse.journal.service;

import java.util.List;

/**
 * Outcome of a recurring journal generation run.
 *
 * @param journals journals generated
 * @param errors occurrences that could not be generated (template, date and reason)
 */
public record GenerationResult(List<GeneratedJournal> journals, List<String> errors) {

  /** Canonical constructor copying the lists. */
  public GenerationResult {
    journals = List.copyOf(journals);
    errors = List.copyOf(errors);
  }

  /**
   * Number of journals generated.
   *
   * @return count
   */
  public int generated() {
    return journals.size();
  }

  /**
   * Summary for the job monitor.
   *
   * @return message
   */
  public String message() {
    String text = generated() + " journal(s) generated";
    return errors.isEmpty()
        ? text
        : text + "; " + errors.size() + " failed: " + String.join("; ", errors);
  }
}
