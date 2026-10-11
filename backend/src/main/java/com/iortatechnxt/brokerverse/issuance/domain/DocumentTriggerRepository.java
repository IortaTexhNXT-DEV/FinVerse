package com.iortatechnxt.brokerverse.issuance.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Document trigger rules. */
public interface DocumentTriggerRepository extends JpaRepository<DocumentTrigger, Long> {

  /**
   * Active rules of a document type.
   *
   * @param documentType document type (list DOCUMENT_TYPE)
   * @return rules
   */
  List<DocumentTrigger> findByDocumentTypeAndActiveTrue(String documentType);

  /**
   * Every rule by document type.
   *
   * @return rules
   */
  List<DocumentTrigger> findAllByOrderByDocumentTypeAscIdAsc();
}
