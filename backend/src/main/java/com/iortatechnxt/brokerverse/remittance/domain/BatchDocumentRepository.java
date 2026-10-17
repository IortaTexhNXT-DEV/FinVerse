package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.DocumentKind;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Documents stored on remittance batches. */
public interface BatchDocumentRepository extends JpaRepository<BatchDocument, Long> {

  /**
   * A document of a batch.
   *
   * @param batchId batch
   * @param kind kind
   * @return document
   */
  Optional<BatchDocument> findByBatchIdAndKind(Long batchId, DocumentKind kind);
}
