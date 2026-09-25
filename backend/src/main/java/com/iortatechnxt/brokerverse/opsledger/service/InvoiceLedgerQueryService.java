package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.domain.InvoiceFlag;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceAdjustmentTotal;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceAdjustmentTotalRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovementRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceStatusChange;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceStatusChangeRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read contract of the Operations ledger for every Operations module and screen: invoices with
 * components and shares, search, movements (RMTID.038), status history (RMTID.032) and cumulative
 * adjustments (ADJID.028).
 */
@Service
@Transactional(readOnly = true)
public class InvoiceLedgerQueryService {

  private static final String CLASSIFICATION = "classification";
  private static final String BOOKING_DATE = "bookingDate";

  private final OpsInvoiceRepository invoices;
  private final OpsInvoiceMovementRepository movements;
  private final OpsInvoiceStatusChangeRepository history;
  private final OpsInvoiceAdjustmentTotalRepository totals;

  /**
   * Creates the service.
   *
   * @param invoices invoices
   * @param movements movements
   * @param history status history
   * @param totals cumulative adjustments
   */
  public InvoiceLedgerQueryService(
      OpsInvoiceRepository invoices,
      OpsInvoiceMovementRepository movements,
      OpsInvoiceStatusChangeRepository history,
      OpsInvoiceAdjustmentTotalRepository totals) {
    this.invoices = invoices;
    this.movements = movements;
    this.history = history;
    this.totals = totals;
  }

  /**
   * An invoice with its components and shares.
   *
   * @param invoiceNo invoice number
   * @return invoice
   */
  public OpsInvoice require(String invoiceNo) {
    return find(invoiceNo)
        .orElseThrow(() -> new ResourceNotFoundException("Operations invoice", invoiceNo));
  }

  /**
   * An invoice with its components and shares, if it is in the ledger.
   *
   * @param invoiceNo invoice number
   * @return invoice
   */
  public Optional<OpsInvoice> find(String invoiceNo) {
    return invoices.findByInvoiceNo(invoiceNo);
  }

  /**
   * Invoices of an account (ARN look-up, ADJID.024), oldest first, loaded.
   *
   * @param arn Account Reference Number
   * @return invoices
   */
  public List<OpsInvoice> forArn(String arn) {
    List<OpsInvoice> list = invoices.findByArnOrderByPolicyYearAscIdAsc(arn);
    list.forEach(InvoiceLedgerQueryService::load);
    return list;
  }

  /**
   * The family of an invoice (DIS 3.27.2, ACSL 2.16.0): every invoice sharing its root invoice
   * number (the original booking, its endorsements and cancellations), root first, loaded.
   *
   * @param invoiceNo any invoice of the family
   * @return the family; empty when the invoice is not in the ledger
   */
  public List<OpsInvoice> family(String invoiceNo) {
    List<OpsInvoice> list =
        invoices
            .findByInvoiceNo(invoiceNo)
            .map(i -> invoices.findByRootInvoiceNoOrderByIdAsc(i.getRootInvoiceNo()))
            .orElseGet(List::of);
    list.forEach(InvoiceLedgerQueryService::load);
    return list;
  }

  /**
   * Searches invoices (collections not loaded).
   *
   * @param search criteria
   * @param pageable page and sort
   * @return invoices
   */
  public Page<OpsInvoice> search(LedgerSearch search, Pageable pageable) {
    return invoices.findAll(specification(search), pageable);
  }

  /**
   * Searches invoices with their components and shares loaded (batch use by the modules).
   *
   * @param search criteria
   * @param pageable page and sort
   * @return invoices, loaded
   */
  public Page<OpsInvoice> searchLoaded(LedgerSearch search, Pageable pageable) {
    Page<OpsInvoice> page = search(search, pageable);
    page.forEach(InvoiceLedgerQueryService::load);
    return page;
  }

  /**
   * Movements of an invoice in posting order.
   *
   * @param invoiceNo invoice number
   * @return movements
   */
  public List<OpsInvoiceMovement> movements(String invoiceNo) {
    return movements.findByInvoiceIdOrderByIdAsc(require(invoiceNo).getId());
  }

  /**
   * Movements of one business transaction (e.g. every application of a receipt).
   *
   * @param sourceModule source module
   * @param sourceRef source reference
   * @return movements
   */
  public List<OpsInvoiceMovement> movementsOf(String sourceModule, String sourceRef) {
    return movements.findBySourceModuleAndSourceRefOrderByIdAsc(sourceModule, sourceRef);
  }

  /**
   * Status, flag and lock history of an invoice, oldest first.
   *
   * @param invoiceNo invoice number
   * @return changes
   */
  public List<OpsInvoiceStatusChange> history(String invoiceNo) {
    return history.findByInvoiceIdOrderByIdAsc(require(invoiceNo).getId());
  }

  /**
   * Cumulative adjustments of an original invoice (ADJID.028).
   *
   * @param originalInvoiceNo original invoice number
   * @return totals, empty when never adjusted
   */
  public Optional<OpsInvoiceAdjustmentTotal> adjustmentTotal(String originalInvoiceNo) {
    return totals.findByOriginalInvoiceNo(originalInvoiceNo);
  }

  private static void load(OpsInvoice invoice) {
    invoice.loadCollections();
  }

  private static Specification<OpsInvoice> specification(LedgerSearch s) {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      where.add(cb.equal(root.get("companyId"), s.companyId()));
      equalsIfSet(where, cb, root, "insurerCode", s.insurerCode());
      equalsIfSet(where, cb, root, "clientCode", s.clientCode());
      if (s.paymentStatus() != null) {
        where.add(cb.equal(root.get("paymentStatus"), s.paymentStatus()));
      }
      if (s.remittanceStatus() != null) {
        where.add(cb.equal(root.get("remittanceStatus"), s.remittanceStatus()));
      }
      if (s.flag() != null) {
        where.add(cb.isTrue(root.get(flagField(s.flag()))));
      }
      if (s.locked() != null) {
        where.add(
            s.locked() ? cb.isNotNull(root.get("lockOwner")) : cb.isNull(root.get("lockOwner")));
      }
      if (s.directPayment() != null) {
        where.add(cb.equal(root.get("dpFlag"), s.directPayment()));
      }
      dates(where, cb, root, s);
      text(where, cb, root, s.text());
      return cb.and(where.toArray(Predicate[]::new));
    };
  }

  private static void dates(
      List<Predicate> where, CriteriaBuilder cb, Root<OpsInvoice> root, LedgerSearch s) {
    if (s.from() != null) {
      where.add(cb.greaterThanOrEqualTo(root.get(CLASSIFICATION).get(BOOKING_DATE), s.from()));
    }
    if (s.to() != null) {
      where.add(cb.lessThanOrEqualTo(root.get(CLASSIFICATION).get(BOOKING_DATE), s.to()));
    }
  }

  private static void text(
      List<Predicate> where, CriteriaBuilder cb, Root<OpsInvoice> root, String text) {
    if (text == null || text.isBlank()) {
      return;
    }
    String like = "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
    where.add(
        cb.or(
            cb.like(cb.lower(root.get("invoiceNo")), like),
            cb.like(cb.lower(root.get("arn")), like),
            cb.like(cb.lower(root.get("policyNo")), like),
            cb.like(cb.lower(root.get("clientCode")), like),
            cb.like(cb.lower(root.get("assuredName")), like)));
  }

  private static void equalsIfSet(
      List<Predicate> where, CriteriaBuilder cb, Root<OpsInvoice> root, String field, String v) {
    if (v != null && !v.isBlank()) {
      where.add(cb.equal(root.get(field), v.strip()));
    }
  }

  private static String flagField(InvoiceFlag flag) {
    return switch (flag) {
      case HOLD -> "holdFlag";
      case PENDING_NEG_ADJ -> "pendingNegAdj";
      case WRITTEN_OFF -> "writtenOff";
      case CANCELLED -> "cancelled";
      case ESTIMATED -> "estimated";
    };
  }
}
