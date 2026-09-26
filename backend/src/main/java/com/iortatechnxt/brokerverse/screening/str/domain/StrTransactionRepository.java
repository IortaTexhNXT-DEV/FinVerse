package com.iortatechnxt.brokerverse.screening.str.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** STR transactions (SNSRP-705). */
public interface StrTransactionRepository extends JpaRepository<StrTransaction, Long> {

  /**
   * The transactions of an STR in order.
   *
   * @param strId STR
   * @return transactions
   */
  List<StrTransaction> findByStrIdOrderByIdAsc(Long strId);
}
