package com.iortatechnxt.brokerverse.productmaint.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Client responses to proposals. */
public interface ClientResponseRepository extends JpaRepository<ClientResponse, Long> {

  /**
   * The responses of a record, newest first.
   *
   * @param recordType record type
   * @param recordId record id
   * @return responses
   */
  List<ClientResponse> findByRecordTypeAndRecordIdOrderByIdDesc(String recordType, Long recordId);
}
