package com.iortatechnxt.brokerverse.journal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link RecurringJournalTemplate}. */
public interface RecurringJournalTemplateRepository
    extends JpaRepository<RecurringJournalTemplate, Long> {

  /**
   * Templates of a company.
   *
   * @param companyId company
   * @return templates ordered by name
   */
  List<RecurringJournalTemplate> findByCompanyIdOrderByName(Long companyId);

  /**
   * Active templates of all companies.
   *
   * @return templates ordered by id
   */
  List<RecurringJournalTemplate> findByActiveTrueOrderById();

  /**
   * Checks whether a name is taken within a company.
   *
   * @param companyId company
   * @param name name
   * @return true when taken
   */
  boolean existsByCompanyIdAndNameIgnoreCase(Long companyId, String name);
}
