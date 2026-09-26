package com.iortatechnxt.brokerverse.booking.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Service invoices and credits. */
public interface ServiceInvoiceRepository
    extends JpaRepository<ServiceInvoice, Long>, JpaSpecificationExecutor<ServiceInvoice> {

  /**
   * A service invoice by number.
   *
   * @param siNo number
   * @return service invoice
   */
  Optional<ServiceInvoice> findBySiNo(String siNo);

  /**
   * Service invoices of a booked invoice.
   *
   * @param invoiceNo booked invoice
   * @return service invoices, oldest first
   */
  List<ServiceInvoice> findByInvoiceNoOrderByIdAsc(String invoiceNo);

  /**
   * Service invoices of an account.
   *
   * @param arn account
   * @return service invoices, oldest first
   */
  List<ServiceInvoice> findByArnOrderByIdAsc(String arn);

  /**
   * Credits of a service invoice.
   *
   * @param creditOf service invoice number
   * @return credits
   */
  List<ServiceInvoice> findByCreditOfOrderByIdAsc(String creditOf);
}
