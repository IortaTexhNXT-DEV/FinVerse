package com.iortatechnxt.brokerverse.eb.domain;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Required documents. */
public interface EbRequiredDocumentRepository extends JpaRepository<EbRequiredDocument, Long> {

  /**
   * Requirements of a company.
   *
   * @param companyId company
   * @return requirements
   */
  List<EbRequiredDocument> findByCompanyIdOrderByProcessTypeAscIdAsc(Long companyId);

  /**
   * Requirements in a record status (approval inbox).
   *
   * @param status status
   * @return requirements
   */
  List<EbRequiredDocument> findByRecordStatus(RecordStatus status);
}
