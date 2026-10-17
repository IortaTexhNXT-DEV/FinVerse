package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Rate-scheme exceptions (BRPM.007). */
public interface RateOverrideRepository extends JpaRepository<RateOverride, Long> {

  /**
   * An exception by reference number.
   *
   * @param referenceNo reference number
   * @return exception
   */
  Optional<RateOverride> findByReferenceNo(String referenceNo);

  /**
   * The exceptions of a transaction, newest first.
   *
   * @param transactionRef quotation number or ARN
   * @return exceptions
   */
  List<RateOverride> findByTransactionRefOrderByIdDesc(String transactionRef);

  /**
   * Every exception, newest first.
   *
   * @return exceptions
   */
  List<RateOverride> findAllByOrderByIdDesc();
}
