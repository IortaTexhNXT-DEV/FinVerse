package com.iortatechnxt.brokerverse.eb.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** The EB document register. */
public interface EbDocumentRepository extends JpaRepository<EbDocument, Long> {

  /**
   * The documents of a programme, oldest first.
   *
   * @param programmeId programme
   * @return documents
   */
  List<EbDocument> findByProgrammeIdOrderByIdAsc(Long programmeId);

  /**
   * The versions of one document type of a cycle, latest first.
   *
   * @param cycleId cycle
   * @param documentType document type
   * @return versions
   */
  List<EbDocument> findByCycleIdAndDocumentTypeOrderByVersionNoDesc(
      Long cycleId, String documentType);
}
