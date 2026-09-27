package com.iortatechnxt.brokerverse.collections.legacy.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link LegacyState}. */
public interface LegacyStateRepository extends JpaRepository<LegacyState, Long> {

  /**
   * The live rows of an invoice.
   *
   * @param companyId company
   * @param invoiceNo ledger invoice
   * @return rows in load order
   */
  List<LegacyState> findByCompanyIdAndInvoiceNoAndRolledBackAtIsNullOrderByIdAsc(
      Long companyId, String invoiceNo);

  /**
   * The rows of a batch.
   *
   * @param migrationBatch batch
   * @return rows
   */
  List<LegacyState> findByMigrationBatch(String migrationBatch);
}
