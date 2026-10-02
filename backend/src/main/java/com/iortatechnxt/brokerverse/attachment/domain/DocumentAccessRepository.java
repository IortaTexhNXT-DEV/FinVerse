package com.iortatechnxt.brokerverse.attachment.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Access class rows of document types (V1031). */
public interface DocumentAccessRepository extends JpaRepository<DocumentAccess, Long> {

  /**
   * The rows of some document types.
   *
   * @param documentTypes document types
   * @return rows
   */
  List<DocumentAccess> findByDocumentTypeIn(Collection<String> documentTypes);

  /**
   * The rows of every type, ordered for display.
   *
   * @return rows
   */
  List<DocumentAccess> findAllByOrderByDocumentTypeAscAccessClassAscPermissionAsc();
}
