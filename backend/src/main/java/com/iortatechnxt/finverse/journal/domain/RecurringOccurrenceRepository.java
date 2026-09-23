package com.iortatechnxt.finverse.journal.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link RecurringOccurrence}. */
public interface RecurringOccurrenceRepository extends JpaRepository<RecurringOccurrence, Long> {

  /**
   * Checks whether an occurrence was already generated.
   *
   * @param templateId template
   * @param occurrenceDate date
   * @return true when generated
   */
  boolean existsByTemplateIdAndOccurrenceDate(Long templateId, LocalDate occurrenceDate);

  /**
   * Generation history of a template.
   *
   * @param templateId template
   * @return occurrences, newest first
   */
  List<RecurringOccurrence> findByTemplateIdOrderByOccurrenceDateDesc(Long templateId);
}
