package com.iortatechnxt.finverse.ledger.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Read access to posted {@link LedgerEntry} rows (writes happen only in the posting engine). */
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

  /**
   * Checks whether an account has postings.
   *
   * @param accountId account
   * @return true when entries exist
   */
  boolean existsByAccountId(Long accountId);

  /**
   * Lists entries of a batch.
   *
   * @param batchId batch
   * @return entries ordered by line
   */
  List<LedgerEntry> findByBatchIdOrderByLineNo(Long batchId);

  /**
   * Lists the entries of an account in a date range (account statement).
   *
   * @param companyId company
   * @param accountId account
   * @param branchId branch or null for all
   * @param from start
   * @param to end
   * @return entries in posting order
   */
  @Query(
      """
      select e from LedgerEntry e
      where e.companyId = :companyId and e.accountId = :accountId
        and (:branchId is null or e.branchId = :branchId)
        and e.valueDate between :from and :to
      order by e.valueDate, e.id
      """)
  List<LedgerEntry> statement(
      @Param("companyId") Long companyId,
      @Param("accountId") Long accountId,
      @Param("branchId") Long branchId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);
}
