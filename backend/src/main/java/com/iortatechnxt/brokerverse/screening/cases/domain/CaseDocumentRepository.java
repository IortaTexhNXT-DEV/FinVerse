package com.iortatechnxt.brokerverse.screening.cases.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Case document metadata (SNSRP-601). */
public interface CaseDocumentRepository extends JpaRepository<CaseDocument, Long> {

  /**
   * The documents of a case, in upload order.
   *
   * @param caseId case
   * @return documents
   */
  List<CaseDocument> findByCaseIdOrderByIdAsc(Long caseId);

  /**
   * Number of documents of a type on a case (the sequence of the naming convention).
   *
   * @param caseId case
   * @param documentType document type
   * @return count
   */
  long countByCaseIdAndDocumentType(Long caseId, String documentType);
}
