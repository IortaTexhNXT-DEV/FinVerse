package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.ApplicationRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptLine;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Receipt search with the ten criteria of CSHID.010 (AR / OR number, client number, invoice number,
 * payor, assured, amount, date of issuance, policy number, insurer), single or combined, exact on
 * numbers and partial on names, paged. Every search is logged (user, criteria, time, result count;
 * OQ47).
 */
@Service
@Transactional
public class ReceiptSearchService {

  private static final String LOG =
      "insert into csh_search_log (company_id, username, criteria, result_count, searched_at)"
          + " values (?, ?, ?, ?, ?)";
  private static final String RECEIPT_ID = "receiptId";
  private static final String INVOICE_NO = "invoiceNo";
  private static final int MAX_CRITERIA = 1000;
  private static final String PRINTED = "printedCount";
  private static final String LOG_WHERE =
      " from csh_search_log l left join sec_user u on u.username = l.username"
          + " where l.company_id = ? and (cast(? as varchar) is null"
          + " or lower(l.username) like lower(?) or lower(coalesce(u.full_name, '')) like lower(?))"
          + " and (cast(? as timestamptz) is null or l.searched_at >= ?)"
          + " and (cast(? as timestamptz) is null or l.searched_at < ?)";
  private static final String LOG_COUNT = "select count(*)" + LOG_WHERE;
  private static final String LOG_PAGE =
      "select l.id, l.searched_at, l.username, coalesce(u.full_name, l.username) as name,"
          + " l.criteria, l.result_count"
          + LOG_WHERE
          + " order by l.searched_at desc, l.id desc limit ? offset ?";
  private static final List<ReceiptSource> SYSTEM_SOURCES =
      List.of(
          ReceiptSource.UPLOAD,
          ReceiptSource.PDC,
          ReceiptSource.PICKUP,
          ReceiptSource.SETTLEMENT,
          ReceiptSource.COMMISSION_UPLOAD);

  private final CashReceiptRepository receipts;
  private final ApplicationRepository applications;
  private final InvoiceLedgerQueryService invoices;
  private final JdbcTemplate jdbc;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param receipts receipts
   * @param applications accounts paid by an AR
   * @param invoices insurer of an account
   * @param jdbc JDBC (search log)
   * @param currentUser current user
   * @param clock clock
   */
  public ReceiptSearchService(
      CashReceiptRepository receipts,
      ApplicationRepository applications,
      InvoiceLedgerQueryService invoices,
      JdbcTemplate jdbc,
      CurrentUser currentUser,
      Clock clock) {
    this.receipts = receipts;
    this.applications = applications;
    this.invoices = invoices;
    this.jdbc = jdbc;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Searches receipts and logs the search.
   *
   * @param criteria criteria
   * @param pageable page and sort
   * @return receipts
   */
  public Page<Receipt> search(ReceiptCriteria criteria, Pageable pageable) {
    Page<Receipt> page = receipts.findAll(specification(criteria), pageable);
    String text = criteria.describe();
    jdbc.update(
        LOG,
        criteria.companyId(),
        currentUser.username(),
        text.length() > MAX_CRITERIA ? text.substring(0, MAX_CRITERIA) : text,
        page.getTotalElements(),
        Timestamp.from(clock.instant()));
    return page;
  }

  /**
   * The insurer of a receipt for the print list (FRS.CSH.02.04.04): the insurer of the first
   * account an AR paid, or of the lines of an OR.
   *
   * @param receipt receipt (detached)
   * @return insurer code, null when none
   */
  @Transactional(readOnly = true)
  public String insurerOf(Receipt receipt) {
    Receipt r = receipts.findById(receipt.getId()).orElse(receipt);
    if (r.getKind() == ReceiptKind.OR) {
      return r.getLines().stream()
          .map(ReceiptLine::getInsurerCode)
          .filter(code -> code != null && !code.isBlank())
          .findFirst()
          .orElse(r.getPayorCode());
    }
    return applications.findByReceiptIdOrderByIdAsc(r.getId()).stream()
        .filter(Application::isActive)
        .findFirst()
        .flatMap(a -> invoices.find(a.getInvoiceNo()))
        .map(OpsInvoice::getInsurerCode)
        .orElse(null);
  }

  /**
   * The search log (FRS.CSH.08.01.04): the searches of a company, of a user and a period.
   *
   * @param companyId company
   * @param user part of the user name or login, null for every user
   * @param from from date, may be null
   * @param to to date, may be null
   * @param pageable page
   * @return searches, latest first
   */
  @Transactional(readOnly = true)
  public Page<SearchLogRow> log(
      Long companyId, String user, LocalDate from, LocalDate to, Pageable pageable) {
    String like = present(user) ? "%" + user.strip() + "%" : null;
    Timestamp start =
        from == null ? null : Timestamp.from(from.atStartOfDay(BusinessClock.zone()).toInstant());
    Timestamp end =
        to == null
            ? null
            : Timestamp.from(to.plusDays(1).atStartOfDay(BusinessClock.zone()).toInstant());
    Object[] args = {companyId, like, like, like, start, start, end, end};
    Long total = jdbc.queryForObject(LOG_COUNT, Long.class, args);
    Object[] paged = Arrays.copyOf(args, args.length + 2);
    paged[args.length] = pageable.getPageSize();
    paged[args.length + 1] = pageable.getOffset();
    List<SearchLogRow> rows =
        jdbc.query(
            LOG_PAGE,
            (rs, n) ->
                new SearchLogRow(
                    rs.getLong("id"),
                    rs.getTimestamp("searched_at").toInstant(),
                    rs.getString("name"),
                    rs.getString("criteria"),
                    rs.getLong("result_count")),
            paged);
    return new PageImpl<>(rows, pageable, total == null ? 0 : total);
  }

  /**
   * A search of the log.
   *
   * @param id id
   * @param searchedAt date and time
   * @param user name of the user
   * @param criteria criteria in words
   * @param results number of results
   */
  public record SearchLogRow(
      long id, java.time.Instant searchedAt, String user, String criteria, long results) {}

  private static Specification<Receipt> specification(ReceiptCriteria c) {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      where.add(cb.equal(root.get("companyId"), c.companyId()));
      exact(where, cb, root.get("receiptNo"), c.receiptNo());
      exact(where, cb, root.get("payorCode"), c.clientCode());
      like(where, cb, root.get("payorName"), c.payor());
      like(where, cb, root.get("assuredName"), c.assured());
      facts(where, root, cb, c);
      printing(where, root, cb, c.print());
      related(where, root, query, cb, c);
      query.distinct(true);
      return cb.and(where.toArray(Predicate[]::new));
    };
  }

  private static void facts(
      List<Predicate> where, Root<Receipt> root, CriteriaBuilder cb, ReceiptCriteria c) {
    if (c.kind() != null) {
      where.add(cb.equal(root.get("kind"), c.kind()));
    }
    if (c.status() != null) {
      where.add(cb.equal(root.get("status"), c.status()));
    }
    if (c.amount() != null) {
      where.add(cb.equal(root.get("amount"), c.amount()));
    }
    if (c.from() != null) {
      where.add(cb.greaterThanOrEqualTo(root.get("receiptDate"), c.from()));
    }
    if (c.to() != null) {
      where.add(cb.lessThanOrEqualTo(root.get("receiptDate"), c.to()));
    }
  }

  private static void printing(
      List<Predicate> where, Root<Receipt> root, CriteriaBuilder cb, PrintFilter p) {
    if (p == null) {
      return;
    }
    if (p.branchId() != null) {
      where.add(cb.equal(root.get("branchId"), p.branchId()));
    }
    if (p.printed() != null) {
      where.add(
          p.printed() ? cb.greaterThan(root.get(PRINTED), 0) : cb.equal(root.get(PRINTED), 0));
    }
    if (p.systemOnly()) {
      where.add(root.get("source").in(SYSTEM_SOURCES));
    }
    like(where, cb, root.get("receiptNo"), p.numberPart());
  }

  private static void related(
      List<Predicate> where,
      Root<Receipt> root,
      CriteriaQuery<?> query,
      CriteriaBuilder cb,
      ReceiptCriteria c) {
    if (present(c.invoiceNo())) {
      where.add(
          cb.or(
              appliedTo(root, query, cb, c.invoiceNo()),
              onLine(root, cb, INVOICE_NO, c.invoiceNo())));
    }
    if (present(c.policyNo())) {
      where.add(cb.exists(invoiceField(root, query, cb, "policyNo", c.policyNo())));
    }
    if (present(c.insurer())) {
      where.add(
          cb.or(
              onLine(root, cb, "insurerCode", c.insurer()),
              cb.exists(invoiceField(root, query, cb, "insurerCode", c.insurer())),
              cb.equal(
                  cb.upper(root.get("payorCode")), c.insurer().strip().toUpperCase(Locale.ROOT))));
    }
  }

  private static Predicate appliedTo(
      Root<Receipt> root, CriteriaQuery<?> query, CriteriaBuilder cb, String invoiceNo) {
    Subquery<Long> sub = query.subquery(Long.class);
    Root<Application> a = sub.from(Application.class);
    sub.select(a.get("id"))
        .where(
            cb.equal(a.get(RECEIPT_ID), root.get("id")),
            cb.equal(a.get(INVOICE_NO), invoiceNo.strip()));
    return cb.exists(sub);
  }

  private static Predicate onLine(
      Root<Receipt> root, CriteriaBuilder cb, String field, String value) {
    Join<Object, Object> line = root.join("lines", JoinType.LEFT);
    return cb.equal(line.get(field), value.strip());
  }

  private static Subquery<Long> invoiceField(
      Root<Receipt> root, CriteriaQuery<?> query, CriteriaBuilder cb, String field, String value) {
    Subquery<Long> sub = query.subquery(Long.class);
    Root<Application> a = sub.from(Application.class);
    Root<OpsInvoice> i = sub.from(OpsInvoice.class);
    sub.select(a.get("id"))
        .where(
            cb.equal(a.get(RECEIPT_ID), root.get("id")),
            cb.equal(i.get(INVOICE_NO), a.get(INVOICE_NO)),
            cb.equal(i.get(field), value.strip()));
    return sub;
  }

  private static void exact(
      List<Predicate> where, CriteriaBuilder cb, Path<String> path, String value) {
    if (present(value)) {
      where.add(cb.equal(path, value.strip()));
    }
  }

  private static void like(
      List<Predicate> where, CriteriaBuilder cb, Path<String> path, String value) {
    if (present(value)) {
      where.add(cb.like(cb.lower(path), "%" + value.strip().toLowerCase(Locale.ROOT) + "%"));
    }
  }

  private static boolean present(String value) {
    return value != null && !value.isBlank();
  }

  /**
   * Search criteria (CSHID.010).
   *
   * @param companyId company
   * @param receiptNo AR or OR number
   * @param clientCode client (payor) number
   * @param invoiceNo invoice number
   * @param payor payor name (partial)
   * @param assured assured name (partial)
   * @param amount amount
   * @param from issued from
   * @param to issued to
   * @param policyNo policy number
   * @param insurer insurer code
   * @param kind AR or OR
   * @param status status
   * @param print print filters (receipting branch, printed or not, system receipts, part of the
   *     number), may be null
   */
  public record ReceiptCriteria(
      Long companyId,
      String receiptNo,
      String clientCode,
      String invoiceNo,
      String payor,
      String assured,
      BigDecimal amount,
      LocalDate from,
      LocalDate to,
      String policyNo,
      String insurer,
      ReceiptKind kind,
      ReceiptStatus status,
      PrintFilter print) {

    /**
     * The criteria given, in words, for the search log (FRS.CSH.08.01.04).
     *
     * @return criteria, e.g. "Payor: santos; Issued from: 2026-10-01"
     */
    public String describe() {
      List<String> parts = new ArrayList<>();
      add(parts, "Receipt number", receiptNo);
      add(parts, "Client number", clientCode);
      add(parts, "Invoice number", invoiceNo);
      add(parts, "Payor", payor);
      add(parts, "Assured", assured);
      add(parts, "Amount", amount);
      add(parts, "Issued from", from);
      add(parts, "Issued to", to);
      add(parts, "Policy number", policyNo);
      add(parts, "Insurer", insurer);
      add(parts, "Receipt type", kind);
      add(parts, "Status", status);
      if (print != null) {
        add(parts, "Receipting branch", print.branchId());
        add(parts, "Printed", print.printed());
        add(parts, "Number", print.numberPart());
      }
      return parts.isEmpty() ? "All receipts" : String.join("; ", parts);
    }

    private static void add(List<String> parts, String label, Object value) {
      if (value != null && !value.toString().isBlank()) {
        parts.add(label + ": " + value);
      }
    }
  }

  /**
   * The filters of the printing screens (FRS.CSH.02.04.02).
   *
   * @param branchId receipting branch
   * @param printed true for printed receipts (re-printing), false for the print queue (never
   *     printed), null for both
   * @param systemOnly only receipts the system generated (uploaded payments, matured post-dated
   *     checks, check pick-ups, automatic ORs)
   * @param numberPart full or partial AR or OR number (re-printing search)
   */
  public record PrintFilter(
      Long branchId, Boolean printed, boolean systemOnly, String numberPart) {}
}
