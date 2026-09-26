package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoiceRepository;
import com.iortatechnxt.brokerverse.booking.domain.SiKind;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service invoice register reads (BRNB.100b): search, invoices of a booking, stored PDF. */
@Service
@Transactional(readOnly = true)
public class ServiceInvoiceRegister {

  private final ServiceInvoiceRepository serviceInvoices;

  /**
   * Creates the register.
   *
   * @param serviceInvoices service invoices
   */
  public ServiceInvoiceRegister(ServiceInvoiceRepository serviceInvoices) {
    this.serviceInvoices = serviceInvoices;
  }

  /**
   * Service invoice register (BRNB.100b): by number, invoice, ARN or recipient.
   *
   * @param companyId company
   * @param text number, invoice number, ARN or recipient fragment
   * @param kind invoice or credit, null for both
   * @param pageable page
   * @return service invoices, newest first
   */
  public Page<ServiceInvoice> search(Long companyId, String text, SiKind kind, Pageable pageable) {
    Specification<ServiceInvoice> spec =
        (root, query, cb) -> {
          List<Predicate> where = new ArrayList<>();
          where.add(cb.equal(root.get("companyId"), companyId));
          if (kind != null) {
            where.add(cb.equal(root.get("kind"), kind));
          }
          if (text != null && !text.isBlank()) {
            String like = "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
            where.add(
                cb.or(
                    cb.like(cb.lower(root.get("siNo")), like),
                    cb.like(cb.lower(root.get("invoiceNo")), like),
                    cb.like(cb.lower(root.get("arn")), like),
                    cb.like(cb.lower(root.get("recipientName")), like),
                    cb.like(cb.lower(root.get("recipientCode")), like)));
          }
          return cb.and(where.toArray(Predicate[]::new));
        };
    return serviceInvoices.findAll(spec, pageable);
  }

  /**
   * Service invoices of a booked invoice.
   *
   * @param invoiceNo booked invoice
   * @return service invoices and credits
   */
  public List<ServiceInvoice> forInvoice(String invoiceNo) {
    return serviceInvoices.findByInvoiceNoOrderByIdAsc(invoiceNo);
  }

  /**
   * The stored PDF of a service invoice.
   *
   * @param id service invoice
   * @return PDF bytes
   */
  public byte[] document(Long id) {
    return serviceInvoices
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ServiceInvoiceDispatch.ENTITY, id))
        .getDocument();
  }
}
