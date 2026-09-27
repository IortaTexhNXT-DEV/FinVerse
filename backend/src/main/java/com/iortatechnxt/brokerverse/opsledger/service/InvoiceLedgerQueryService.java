package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.domain.InvoiceFlag;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
  private static final String INCEPTION = "inceptionDate";
  private static final Set<MovementType> PAYMENT_TYPES =
      EnumSet.of(MovementType.APPLIED, MovementType.UNAPPLIED, MovementType.LEGACY_PAID);

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
   * Invoices by number, without their collections (links shown in lists).
   *
   * @param invoiceNos invoice numbers
   * @return invoice per number, for the numbers in the ledger
   */
  public Map<String, OpsInvoice> byNumbers(Collection<String> invoiceNos) {
    Map<String, OpsInvoice> found = new HashMap<>();
    if (invoiceNos.isEmpty()) {
      return found;
    }
    invoices.findByInvoiceNoIn(invoiceNos).forEach(i -> found.put(i.getInvoiceNo(), i));
    return found;
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
   * Open legacy invoices by their number in the source system (DATA_MIGRATION_DESIGN 14.1).
   *
   * @param legacyInvoiceNo legacy number
   * @return invoices with components and shares loaded, oldest first
   */
  public List<OpsInvoice> findByLegacyNo(String legacyInvoiceNo) {
    List<OpsInvoice> list = invoices.findByLegacyLegacyInvoiceNoOrderByIdAsc(legacyInvoiceNo);
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
   * Payments of a client since a date (contract of the Customer Servicing Facility, BRCSF-005;
   * CUSTOMER_SERVICING_DESIGN section 11): the applications of receipts to the client's invoices,
   * their reversals and the legacy payments, newest first. One row per invoice and application,
   * with the premium receivable amount (the component rows of one application are added up).
   *
   * @param companyId company
   * @param clientCode client code
   * @param from first value date
   * @return payments, newest first
   */
  public List<ClientPayment> paymentsOfClient(Long companyId, String clientCode, LocalDate from) {
    Map<String, ClientPayment> rows = new LinkedHashMap<>();
    for (Object[] r : movements.clientMovements(companyId, clientCode, PAYMENT_TYPES, from)) {
      OpsInvoiceMovement m = (OpsInvoiceMovement) r[0];
      if (!m.getComponent().isPremiumReceivable()) {
        continue;
      }
      String invoiceNo = (String) r[1];
      String key =
          String.join(
              "|",
              invoiceNo,
              m.getMovementType().name(),
              String.valueOf(m.getSourceRef()),
              String.valueOf(m.getValueDate()));
      rows.merge(
          key,
          new ClientPayment(
              invoiceNo,
              (String) r[2],
              m.getMovementType(),
              m.getSourceRef(),
              m.getArNo(),
              m.getOrNo(),
              m.getValueDate(),
              m.getAmount(),
              m.getPostedAt()),
          ClientPayment::plus);
    }
    return List.copyOf(rows.values());
  }

  /**
   * A payment movement of a client (Customer Servicing Facility payment history).
   *
   * @param invoiceNo invoice paid
   * @param arn account of the invoice
   * @param type APPLIED, UNAPPLIED (reversed) or LEGACY_PAID
   * @param sourceRef application reference of the source module
   * @param arNo acknowledgement receipt number, may be null
   * @param orNo official receipt number, may be null
   * @param valueDate value date
   * @param amount premium receivable amount
   * @param postedAt time posted
   */
  public record ClientPayment(
      String invoiceNo,
      String arn,
      MovementType type,
      String sourceRef,
      String arNo,
      String orNo,
      LocalDate valueDate,
      BigDecimal amount,
      Instant postedAt) {

    ClientPayment plus(ClientPayment other) {
      return new ClientPayment(
          invoiceNo,
          arn,
          type,
          sourceRef,
          arNo == null ? other.arNo : arNo,
          orNo == null ? other.orNo : orNo,
          valueDate,
          amount.add(other.amount),
          postedAt);
    }
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
      assuredInceptionAo(where, cb, root, s);
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
    if (s.origin() != null) {
      where.add(cb.equal(root.get("recordOrigin").get("origin"), s.origin()));
    }
  }

  /** Assured, inception and account officer filters (DIS 3.27.2 invoice search). */
  private static void assuredInceptionAo(
      List<Predicate> where, CriteriaBuilder cb, Root<OpsInvoice> root, LedgerSearch s) {
    if (s.assured() != null && !s.assured().isBlank()) {
      String like = "%" + s.assured().strip().toLowerCase(Locale.ROOT) + "%";
      where.add(cb.like(cb.lower(root.get("assuredName")), like));
    }
    if (s.inceptionFrom() != null) {
      where.add(
          cb.greaterThanOrEqualTo(root.get(CLASSIFICATION).get(INCEPTION), s.inceptionFrom()));
    }
    if (s.inceptionTo() != null) {
      where.add(cb.lessThanOrEqualTo(root.get(CLASSIFICATION).get(INCEPTION), s.inceptionTo()));
    }
    if (s.aoUsername() != null && !s.aoUsername().isBlank()) {
      where.add(cb.equal(root.get(CLASSIFICATION).get("aoUsername"), s.aoUsername().strip()));
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
            cb.like(cb.lower(root.get("assuredName")), like),
            cb.like(cb.lower(root.get("legacy").get("legacyInvoiceNo")), like)));
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
