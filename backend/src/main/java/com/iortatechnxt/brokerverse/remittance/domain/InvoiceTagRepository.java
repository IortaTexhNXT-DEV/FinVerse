package com.iortatechnxt.brokerverse.remittance.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Extraction tags of invoices. */
public interface InvoiceTagRepository extends JpaRepository<InvoiceTag, Long> {

  /**
   * Tags given by a run.
   *
   * @param runId run
   * @param pageable page
   * @return tags
   */
  Page<InvoiceTag> findByRunIdOrderByIdAsc(Long runId, Pageable pageable);

  /**
   * The current (latest) tag of an invoice.
   *
   * @param invoiceNo invoice
   * @return tag
   */
  Optional<InvoiceTag> findFirstByInvoiceNoOrderByIdDesc(String invoiceNo);

  /**
   * Tags of an invoice, newest first.
   *
   * @param invoiceNo invoice
   * @return tags
   */
  List<InvoiceTag> findByInvoiceNoOrderByIdDesc(String invoiceNo);
}
