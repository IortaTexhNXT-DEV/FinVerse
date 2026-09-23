package com.iortatechnxt.finverse.payables.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.party.domain.PartyType;
import com.iortatechnxt.finverse.party.service.PartyService;
import com.iortatechnxt.finverse.payables.domain.InvoiceHeader;
import com.iortatechnxt.finverse.payables.domain.InvoiceLineValues;
import com.iortatechnxt.finverse.payables.domain.InvoicePosting;
import com.iortatechnxt.finverse.payables.domain.InvoiceStatus;
import com.iortatechnxt.finverse.payables.domain.SupplierInvoice;
import com.iortatechnxt.finverse.payables.domain.SupplierInvoiceRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supplier invoice capture and maker-checker approval.
 *
 * <p>Approval posts the invoice (see {@link SupplierInvoicePoster}) in the same transaction as the
 * status change, so an invoice is either approved with its journals and open item, or not at all.
 */
@Service
@Transactional
public class SupplierInvoiceService {

  /** Party types that can bill the company (vendor sub-ledger). */
  public static final Set<PartyType> VENDORS =
      EnumSet.of(PartyType.SUPPLIER, PartyType.GARAGE, PartyType.SURVEYOR);

  private static final String ENTITY = "SupplierInvoice";
  private static final Set<InvoiceStatus> LIVE =
      EnumSet.of(InvoiceStatus.DRAFT, InvoiceStatus.PENDING_APPROVAL, InvoiceStatus.APPROVED);

  private final SupplierInvoiceRepository invoices;
  private final PartyService parties;
  private final ExpenseAccountValidator validator;
  private final SupplierInvoicePoster poster;
  private final PayablesSupport support;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param invoices repository
   * @param parties party master
   * @param validator expense line validator
   * @param poster invoice posting
   * @param support shared helpers
   * @param audit audit trail
   * @param clock clock
   */
  public SupplierInvoiceService(
      SupplierInvoiceRepository invoices,
      PartyService parties,
      ExpenseAccountValidator validator,
      SupplierInvoicePoster poster,
      PayablesSupport support,
      AuditTrailService audit,
      Clock clock) {
    this.invoices = invoices;
    this.parties = parties;
    this.validator = validator;
    this.poster = poster;
    this.support = support;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Searches invoices.
   *
   * @param companyId company
   * @param status status filter or null
   * @param partyCode supplier filter or null
   * @param from from date
   * @param to to date
   * @param pageable page
   * @return page
   */
  @Transactional(readOnly = true)
  public Page<SupplierInvoice> search(
      Long companyId,
      InvoiceStatus status,
      String partyCode,
      LocalDate from,
      LocalDate to,
      Pageable pageable) {
    return invoices.search(companyId, status, partyCode, from, to, pageable);
  }

  /**
   * Gets an invoice with its lines.
   *
   * @param id id
   * @return invoice
   */
  @Transactional(readOnly = true)
  public SupplierInvoice get(Long id) {
    return invoices
        .findWithLinesById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Supplier invoice", id));
  }

  /**
   * Captures a draft invoice.
   *
   * @param c values
   * @return invoice
   */
  public SupplierInvoice create(InvoiceCommand c) {
    Party supplier = parties.requireActive(c.companyId(), c.partyCode(), VENDORS);
    requireNotDuplicate(c, supplier, null);
    validateLines(c);
    LocalDate date = c.invoiceDate();
    SupplierInvoice invoice =
        new SupplierInvoice(support.nextNumber("SI", c.branchId(), date), header(c, supplier));
    invoice.replaceLines(c.lines());
    SupplierInvoice saved = invoices.save(invoice);
    audit.record(
        ENTITY,
        saved.getDocumentNo(),
        AuditAction.CREATE,
        "Captured invoice " + c.supplierInvoiceNo() + " of " + supplier.getCode());
    return saved;
  }

  /**
   * Updates a draft invoice.
   *
   * @param id id
   * @param c values (supplier cannot change)
   * @return invoice
   */
  public SupplierInvoice update(Long id, InvoiceCommand c) {
    SupplierInvoice invoice = get(id);
    Party supplier = parties.get(invoice.getPartyId());
    requireNotDuplicate(c, supplier, invoice);
    validateLines(c);
    invoice.updateHeader(header(c, supplier));
    invoice.clearLines();
    invoices.flush();
    invoice.replaceLines(c.lines());
    audit.record(ENTITY, invoice.getDocumentNo(), AuditAction.UPDATE, "Updated draft invoice");
    return invoice;
  }

  /**
   * Submits a draft for approval.
   *
   * @param id id
   * @return invoice
   */
  public SupplierInvoice submit(Long id) {
    SupplierInvoice invoice = get(id);
    invoice.submit(support.user(), clock.instant());
    audit.record(ENTITY, invoice.getDocumentNo(), AuditAction.SUBMIT, "Submitted for approval");
    return invoice;
  }

  /**
   * Approves and posts an invoice (checker).
   *
   * @param id id
   * @return invoice
   */
  public SupplierInvoice approve(Long id) {
    SupplierInvoice invoice = get(id);
    String checker =
        support.checker(
            invoice,
            support.toBase(
                invoice.getCompanyId(),
                invoice.getCurrency(),
                invoice.getPayableAmount(),
                invoice.getInvoiceDate()));
    parties.requireActive(invoice.getCompanyId(), invoice.getPartyCode(), VENDORS);
    InvoicePosting posting = poster.post(invoice);
    invoice.approve(checker, clock.instant(), posting);
    audit.record(
        ENTITY,
        invoice.getDocumentNo(),
        AuditAction.POST,
        "Approved and posted " + String.join(", ", posting.batchNos()));
    return invoice;
  }

  /**
   * Returns a submitted invoice to draft.
   *
   * @param id id
   * @param reason reason
   * @return invoice
   */
  public SupplierInvoice reject(Long id, String reason) {
    SupplierInvoice invoice = get(id);
    invoice.reject(support.user(), reason);
    audit.record(ENTITY, invoice.getDocumentNo(), AuditAction.REJECT, reason);
    return invoice;
  }

  /**
   * Cancels an unapproved invoice.
   *
   * @param id id
   * @param reason reason
   * @return invoice
   */
  public SupplierInvoice cancel(Long id, String reason) {
    SupplierInvoice invoice = get(id);
    invoice.cancel(reason);
    audit.record(ENTITY, invoice.getDocumentNo(), AuditAction.DEACTIVATE, "Cancelled: " + reason);
    return invoice;
  }

  private void validateLines(InvoiceCommand c) {
    for (InvoiceLineValues line : c.lines()) {
      validator.validate(c.companyId(), line.expenseAccountCode(), line.costCenter());
    }
  }

  private void requireNotDuplicate(InvoiceCommand c, Party supplier, SupplierInvoice current) {
    boolean sameNumber =
        current != null && current.getSupplierInvoiceNo().equals(c.supplierInvoiceNo());
    if (!sameNumber
        && invoices.existsByCompanyIdAndPartyIdAndSupplierInvoiceNoAndStatusIn(
            c.companyId(), supplier.getId(), c.supplierInvoiceNo(), LIVE)) {
      throw new DuplicateResourceException(
          "Supplier invoice", supplier.getCode() + "/" + c.supplierInvoiceNo());
    }
  }

  private static InvoiceHeader header(InvoiceCommand c, Party supplier) {
    LocalDate due =
        c.dueDate() != null ? c.dueDate() : c.invoiceDate().plusDays(supplier.getCreditDays());
    String currency = c.currency() != null ? c.currency() : supplier.getDefaultCurrency();
    return new InvoiceHeader(
        c.companyId(),
        c.branchId(),
        supplier.getId(),
        supplier.getCode(),
        c.supplierInvoiceNo(),
        c.invoiceDate(),
        due,
        currency,
        c.vatApplicable(),
        supplier.getWithholdingTaxRate(),
        c.narration());
  }
}
