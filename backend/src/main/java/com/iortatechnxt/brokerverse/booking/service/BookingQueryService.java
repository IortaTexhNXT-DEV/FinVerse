package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.booking.domain.BatchRun;
import com.iortatechnxt.brokerverse.booking.domain.BatchRunRepository;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoiceRepository;
import com.iortatechnxt.brokerverse.booking.domain.BookingEndorsement;
import com.iortatechnxt.brokerverse.booking.domain.BookingEndorsementRepository;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceOpenItem;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceStatus;
import com.iortatechnxt.brokerverse.booking.domain.OpenItemRole;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.brokerverse.journal.domain.JournalLine;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItem;
import com.iortatechnxt.brokerverse.subledger.service.OpenItemService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booked invoice reads (BRNB.027). Contract for Operations (OPERATIONS_DESIGN section 2.2): {@link
 * #invoice(String)} returns a booked invoice as the {@link InvoiceBooked} event carried it (feed
 * replay), {@link #invoicesForArn(String)} every booked invoice of an account. The other methods
 * serve the booking screens.
 */
@Service
@Transactional(readOnly = true)
public class BookingQueryService {

  private static final String INVOICE = "Booked invoice";

  private final BookedInvoiceRepository invoices;
  private final BookingEndorsementRepository endorsements;
  private final BatchRunRepository runs;
  private final OpenItemService openItems;
  private final JournalBatchRepository journals;

  /**
   * Creates the service.
   *
   * @param invoices booked invoices
   * @param endorsements endorsements
   * @param runs batch runs
   * @param openItems sub-ledger
   * @param journals posted journals
   */
  public BookingQueryService(
      BookedInvoiceRepository invoices,
      BookingEndorsementRepository endorsements,
      BatchRunRepository runs,
      OpenItemService openItems,
      JournalBatchRepository journals) {
    this.invoices = invoices;
    this.endorsements = endorsements;
    this.runs = runs;
    this.openItems = openItems;
    this.journals = journals;
  }

  /**
   * A booked invoice as published (contract, replay of the Operations feed).
   *
   * @param invoiceNo invoice number
   * @return event data
   */
  public InvoiceBooked invoice(String invoiceNo) {
    BookedInvoice invoice = byNo(invoiceNo);
    if (!invoice.isBooked()) {
      throw new ResourceNotFoundException(INVOICE, invoiceNo);
    }
    return InvoiceBooked.of(invoice);
  }

  /**
   * Every booked invoice of an account, by policy year then booking (contract).
   *
   * @param arn Account Reference Number
   * @return event data of each booked invoice
   */
  public List<InvoiceBooked> invoicesForArn(String arn) {
    return invoices.findByArnOrderByPolicyYearAscIdAsc(arn).stream()
        .filter(BookedInvoice::isBooked)
        .map(InvoiceBooked::of)
        .toList();
  }

  /**
   * An invoice by id, collections loaded.
   *
   * @param id id
   * @return invoice
   */
  public BookedInvoice get(Long id) {
    return loaded(
        invoices.findById(id).orElseThrow(() -> new ResourceNotFoundException(INVOICE, id)));
  }

  /**
   * An invoice by number, collections loaded.
   *
   * @param invoiceNo number
   * @return invoice
   */
  public BookedInvoice byNo(String invoiceNo) {
    return loaded(
        invoices
            .findByInvoiceNo(invoiceNo)
            .orElseThrow(() -> new ResourceNotFoundException(INVOICE, invoiceNo)));
  }

  /**
   * Every invoice of an account: booked, scheduled (multi-year) and cancelled years.
   *
   * @param arn account
   * @return invoices, loaded
   */
  public List<BookedInvoice> schedule(String arn) {
    return invoices.findByArnOrderByPolicyYearAscIdAsc(arn).stream()
        .map(BookingQueryService::loaded)
        .toList();
  }

  /**
   * Searches invoices.
   *
   * @param search criteria
   * @param pageable page and sort
   * @return page of invoices (collections not loaded)
   */
  public Page<BookedInvoice> search(InvoiceSearch search, Pageable pageable) {
    return invoices.findAll(specification(search), pageable);
  }

  /**
   * The open items recorded for an invoice.
   *
   * @param invoice invoice (loaded)
   * @return items with their role
   */
  public List<InvoiceItem> openItems(BookedInvoice invoice) {
    List<InvoiceItem> items = new ArrayList<>();
    for (InvoiceOpenItem link : invoice.getOpenItems()) {
      items.add(new InvoiceItem(link.role(), openItems.get(link.openItemId())));
    }
    return items;
  }

  /**
   * The journal lines posted for an invoice.
   *
   * @param invoice invoice (loaded)
   * @return lines of every journal batch of the invoice
   */
  public List<PostedLine> journalLines(BookedInvoice invoice) {
    List<PostedLine> lines = new ArrayList<>();
    for (String batchNo : invoice.getJournalBatches()) {
      journals
          .findByCompanyIdAndBatchNo(invoice.getCompanyId(), batchNo)
          .ifPresent(batch -> batch.getLines().forEach(l -> lines.add(PostedLine.of(batch, l))));
    }
    return lines;
  }

  /**
   * Endorsements of an account.
   *
   * @param arn account
   * @return endorsements, oldest first
   */
  public List<BookingEndorsement> endorsements(String arn) {
    return endorsements.findByArnOrderByIdAsc(arn);
  }

  /**
   * Endorsements of a company.
   *
   * @param companyId company
   * @param pageable page
   * @return endorsements, newest first
   */
  public Page<BookingEndorsement> endorsements(Long companyId, Pageable pageable) {
    return endorsements.findByCompanyIdOrderByIdDesc(companyId, pageable);
  }

  /**
   * Batch runs of a company.
   *
   * @param companyId company
   * @param pageable page
   * @return runs, newest first
   */
  public Page<BatchRun> runs(Long companyId, Pageable pageable) {
    return runs.findByCompanyIdOrderByIdDesc(companyId, pageable);
  }

  /**
   * A batch run with its rows.
   *
   * @param runNo run number
   * @return run
   */
  public BatchRun run(String runNo) {
    BatchRun run =
        runs.findByRunNo(runNo)
            .orElseThrow(() -> new ResourceNotFoundException("Batch run", runNo));
    Hibernate.initialize(run.getRows());
    return run;
  }

  private static Specification<BookedInvoice> specification(InvoiceSearch s) {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      where.add(cb.equal(root.get("companyId"), s.companyId()));
      where.add(
          cb.equal(root.get("status"), s.status() == null ? InvoiceStatus.BOOKED : s.status()));
      if (s.kind() != null) {
        where.add(cb.equal(root.get("kind"), s.kind()));
      }
      factEquals(where, cb, root, "insurerCode", s.insurerCode());
      factEquals(where, cb, root, "lineCode", s.lineCode());
      if (s.from() != null) {
        where.add(cb.greaterThanOrEqualTo(root.get("bookingDate"), s.from()));
      }
      if (s.to() != null) {
        where.add(cb.lessThanOrEqualTo(root.get("bookingDate"), s.to()));
      }
      if (s.text() != null && !s.text().isBlank()) {
        String like = "%" + s.text().strip().toLowerCase(Locale.ROOT) + "%";
        where.add(
            cb.or(
                cb.like(cb.lower(root.get("invoiceNo")), like),
                cb.like(cb.lower(root.get("arn")), like),
                cb.like(cb.lower(root.get("policyNo")), like),
                cb.like(cb.lower(root.get("facts").get("clientCode")), like),
                cb.like(cb.lower(root.get("facts").get("clientName")), like)));
      }
      return cb.and(where.toArray(Predicate[]::new));
    };
  }

  private static void factEquals(
      List<Predicate> where,
      CriteriaBuilder cb,
      Root<BookedInvoice> root,
      String fact,
      String value) {
    if (value != null && !value.isBlank()) {
      where.add(cb.equal(root.get("facts").get(fact), value.strip()));
    }
  }

  private static BookedInvoice loaded(BookedInvoice invoice) {
    Hibernate.initialize(invoice.getShares());
    Hibernate.initialize(invoice.getJournalBatches());
    Hibernate.initialize(invoice.getOpenItems());
    return invoice;
  }

  /**
   * An open item of an invoice.
   *
   * @param role client premium, insurer DTIP or commission
   * @param item open item
   */
  public record InvoiceItem(OpenItemRole role, OpenItem item) {}

  /**
   * A posted journal line of an invoice.
   *
   * @param batchId journal batch id
   * @param batchNo journal batch number
   * @param accountCode GL account
   * @param accountName account name
   * @param side debit or credit
   * @param amount amount
   * @param partyCode sub-ledger party
   * @param narration narration
   */
  public record PostedLine(
      Long batchId,
      String batchNo,
      String accountCode,
      String accountName,
      BalanceSide side,
      BigDecimal amount,
      String partyCode,
      String narration) {

    static PostedLine of(JournalBatch batch, JournalLine line) {
      return new PostedLine(
          batch.getId(),
          batch.getBatchNo(),
          line.getAccount().getCode(),
          line.getAccount().getName(),
          line.getSide(),
          line.getAmount(),
          line.getPartyCode(),
          line.getNarration());
    }
  }
}
