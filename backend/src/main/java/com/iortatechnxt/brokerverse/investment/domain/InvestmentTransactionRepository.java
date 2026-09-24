package com.iortatechnxt.brokerverse.investment.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link InvestmentTransaction}. */
public interface InvestmentTransactionRepository
    extends JpaRepository<InvestmentTransaction, Long> {

  /**
   * Transaction history of a holding.
   *
   * @param holdingId holding
   * @return transactions in date order
   */
  List<InvestmentTransaction> findByHoldingIdOrderByTxnDateAscIdAsc(Long holdingId);

  /**
   * Transactions of a run.
   *
   * @param runId run
   * @return transactions
   */
  List<InvestmentTransaction> findByRunIdOrderById(Long runId);

  /**
   * Whether a holding already has a transaction of a type on a date.
   *
   * @param holdingId holding
   * @param txnType type
   * @param txnDate date
   * @return true when present
   */
  boolean existsByHoldingIdAndTxnTypeAndTxnDate(
      Long holdingId, TransactionType txnType, LocalDate txnDate);
}
