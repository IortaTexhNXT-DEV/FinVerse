package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * What the policy transaction history shows of an invoice carried over from a legacy system
 * (DATA_MIGRATION_DESIGN 14.1): its legacy system and number, and the journals of the migration
 * that opened it (one per insurer share) or adjusted it at year end.
 */
@Component
@Transactional(readOnly = true)
public class MigratedInvoiceHistory {

  /** Status of an invoice carried over from a legacy system. */
  public static final String MIGRATED = "MIGRATED";

  /** Module of the migration postings. */
  static final String MODULE = "MIGRATION";

  private static final String OPENING_PREFIX = "MIG:INV:";

  private final JournalBatchRepository journals;

  /**
   * Creates the helper.
   *
   * @param journals journal batches
   */
  public MigratedInvoiceHistory(JournalBatchRepository journals) {
    this.journals = journals;
  }

  /**
   * The opening journals of a migrated invoice.
   *
   * @param invoice invoice
   * @return journal numbers, oldest first
   */
  public List<String> openingJournals(OpsInvoice invoice) {
    return journals(invoice, OPENING_PREFIX + invoice.getInvoiceNo() + ":");
  }

  /**
   * The migration journals whose source key starts with a prefix.
   *
   * @param invoice invoice (company)
   * @param prefix start of the source key
   * @return journal numbers, oldest first
   */
  public List<String> journals(OpsInvoice invoice, String prefix) {
    return journals
        .findByCompanyIdAndSourceModuleAndSourceReferenceStartingWithOrderByIdAsc(
            invoice.getCompanyId(), MODULE, prefix)
        .stream()
        .map(JournalBatch::getBatchNo)
        .toList();
  }

  /**
   * The legacy system and invoice number of a migrated invoice.
   *
   * @param invoice invoice
   * @return legacy system and number
   */
  public static String detail(OpsInvoice invoice) {
    String system = invoice.getRecordOrigin().sourceSystem();
    String legacyNo = invoice.getLegacy().legacyInvoiceNo();
    return (system == null ? "Legacy" : system) + " invoice " + (legacyNo == null ? "" : legacyNo);
  }
}
