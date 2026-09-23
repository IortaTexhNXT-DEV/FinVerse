package com.iortatechnxt.finverse.receivables.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link BankStatementLine}. */
public interface BankStatementLineRepository extends JpaRepository<BankStatementLine, Long> {

  /**
   * Lists the lines of a statement.
   *
   * @param statementId statement
   * @return lines in statement order
   */
  List<BankStatementLine> findByStatementIdOrderByLineNoAsc(Long statementId);

  /**
   * Lists the lines of a match.
   *
   * @param matchId match
   * @return lines
   */
  List<BankStatementLine> findByMatchId(Long matchId);

  /**
   * Lists lines by id.
   *
   * @param ids ids
   * @return lines
   */
  List<BankStatementLine> findByIdIn(Collection<Long> ids);

  /**
   * Lists the never matched lines of a bank account dated on or before a date.
   *
   * @param companyId company
   * @param bankAccountCode bank GL account
   * @param asOf date
   * @return lines by value date
   */
  @Query(
      """
      select l from BankStatementLine l
      where l.companyId = :companyId and l.bankAccountCode = :bank and l.valueDate <= :asOf
        and l.matchId is null
      order by l.valueDate, l.statementId, l.lineNo
      """)
  List<BankStatementLine> unmatched(
      @Param("companyId") Long companyId,
      @Param("bank") String bankAccountCode,
      @Param("asOf") LocalDate asOf);

  /**
   * Lists the lines of a bank account dated on or before a date that were not reconciled as of that
   * date (never matched, or matched later).
   *
   * @param companyId company
   * @param bankAccountCode bank GL account
   * @param asOf date
   * @return lines by value date
   */
  @Query(
      """
      select l from BankStatementLine l
      where l.companyId = :companyId and l.bankAccountCode = :bank and l.valueDate <= :asOf
        and (l.matchId is null
             or exists (select m.id from BankMatch m where m.id = l.matchId and m.matchDate > :asOf))
      order by l.valueDate, l.statementId, l.lineNo
      """)
  List<BankStatementLine> unreconciled(
      @Param("companyId") Long companyId,
      @Param("bank") String bankAccountCode,
      @Param("asOf") LocalDate asOf);

  /**
   * Latest statement line on or before a date (its running balance is the bank balance).
   *
   * @param companyId company
   * @param bankAccountCode bank GL account
   * @param asOf date
   * @return lines, latest first (take the first)
   */
  @Query(
      """
      select l from BankStatementLine l
      where l.companyId = :companyId and l.bankAccountCode = :bank and l.valueDate <= :asOf
      order by l.valueDate desc, l.statementId desc, l.lineNo desc
      """)
  List<BankStatementLine> latestFirst(
      @Param("companyId") Long companyId,
      @Param("bank") String bankAccountCode,
      @Param("asOf") LocalDate asOf,
      Pageable page);

  /**
   * Latest statement line on or before a date.
   *
   * @param companyId company
   * @param bankAccountCode bank GL account
   * @param asOf date
   * @return line
   */
  default Optional<BankStatementLine> latestOnOrBefore(
      Long companyId, String bankAccountCode, LocalDate asOf) {
    return latestFirst(companyId, bankAccountCode, asOf, PageRequest.of(0, 1)).stream().findFirst();
  }
}
