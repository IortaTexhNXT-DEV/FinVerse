package com.iortatechnxt.brokerverse.crm.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** KYC documents of clients. */
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

  /**
   * KYC documents of a client, oldest first.
   *
   * @param clientId client
   * @return documents
   */
  List<KycDocument> findByClientIdOrderByIdAsc(Long clientId);
}
