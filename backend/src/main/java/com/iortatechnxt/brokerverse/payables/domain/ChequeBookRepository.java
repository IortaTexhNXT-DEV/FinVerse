package com.iortatechnxt.brokerverse.payables.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link ChequeBook}. */
public interface ChequeBookRepository extends JpaRepository<ChequeBook, Long> {

  /**
   * Lists the cheque books of a bank account.
   *
   * @param bankAccountId bank account
   * @return books, oldest first
   */
  List<ChequeBook> findByBankAccountIdOrderByFirstNo(Long bankAccountId);

  /**
   * Locks the active books of a bank account so concurrent payments never share a leaf.
   *
   * @param bankAccountId bank account
   * @return active books, lowest numbers first
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select b from ChequeBook b
      where b.bankAccountId = :bankAccountId
        and b.status = com.iortatechnxt.brokerverse.payables.domain.ChequeBookStatus.ACTIVE
      order by b.firstNo
      """)
  List<ChequeBook> lockActive(@Param("bankAccountId") Long bankAccountId);
}
