package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Signatures of the approval levels. */
public interface SbmSignatureRepository extends JpaRepository<SbmSignature, Long> {

  /**
   * Signatures of a document, by level.
   *
   * @param documentType IAAF or TOR
   * @param documentId document
   * @return signatures
   */
  List<SbmSignature> findByDocumentTypeAndDocumentIdOrderByLevelAsc(
      String documentType, Long documentId);
}
