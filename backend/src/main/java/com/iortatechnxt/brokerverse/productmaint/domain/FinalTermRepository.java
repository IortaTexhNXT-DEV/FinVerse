package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Final Terms for Proposal. */
public interface FinalTermRepository extends JpaRepository<FinalTerm, Long> {

  /**
   * The Final Terms of a record.
   *
   * @param recordType record type
   * @param recordId record id
   * @return values
   */
  List<FinalTerm> findByRecordTypeAndRecordId(String recordType, Long recordId);
}
