package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Proposal slips per insurer. */
public interface ProposalFileRepository extends JpaRepository<ProposalFile, Long> {

  /**
   * The slips of a record, newest first.
   *
   * @param recordType record type
   * @param recordId record id
   * @return slips
   */
  List<ProposalFile> findByRecordTypeAndRecordIdOrderByIdDesc(String recordType, Long recordId);
}
