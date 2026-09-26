package com.iortatechnxt.brokerverse.collections.billing.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Rendered statements of account. */
public interface BillingDocumentRepository extends JpaRepository<BillingDocument, Long> {

  /**
   * The document of a statement.
   *
   * @param statementId statement
   * @return document
   */
  Optional<BillingDocument> findByStatementId(Long statementId);
}
