package com.iortatechnxt.brokerverse.consolidation.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link IntercompanyTransaction}. */
public interface IntercompanyTransactionRepository
    extends JpaRepository<IntercompanyTransaction, Long> {

  /**
   * Lists the transactions of a company (either side), newest first.
   *
   * @param companyId company
   * @return transactions
   */
  @Query(
      "select t from IntercompanyTransaction t"
          + " where t.creditorCompanyId = :companyId or t.debtorCompanyId = :companyId"
          + " order by t.valueDate desc, t.id desc")
  List<IntercompanyTransaction> findInvolving(@Param("companyId") Long companyId);
}
