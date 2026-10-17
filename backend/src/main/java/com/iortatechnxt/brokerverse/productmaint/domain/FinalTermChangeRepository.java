package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** History of the Final Terms for Proposal. */
public interface FinalTermChangeRepository extends JpaRepository<FinalTermChange, Long> {

  /**
   * The changes of a record, newest first.
   *
   * @param recordType record type
   * @param recordId record id
   * @return changes
   */
  List<FinalTermChange> findByRecordTypeAndRecordIdOrderByIdDesc(String recordType, Long recordId);
}
