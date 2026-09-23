package com.iortatechnxt.finverse.accounting.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link AccountingEventType}. */
public interface AccountingEventTypeRepository extends JpaRepository<AccountingEventType, String> {

  /**
   * Lists event types ordered by category and code.
   *
   * @return event types
   */
  List<AccountingEventType> findAllByOrderByCategoryAscCodeAsc();
}
