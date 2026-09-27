package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
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
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
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

  private final CashReceiptRepository receipts;
  private final JdbcTemplate jdbc;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param receipts receipts
   * @param jdbc JDBC (search log)
   * @param currentUser current user
   * @param clock clock
   */
  public ReceiptSearchService(
      CashReceiptRepository receipts, JdbcTemplate jdbc, CurrentUser currentUser, Clock clock) {
    this.receipts = receipts;
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
    String text = criteria.toString();
    jdbc.update(
        LOG,
        criteria.companyId(),
        currentUser.username(),
        text.length() > MAX_CRITERIA ? text.substring(0, MAX_CRITERIA) : text,
        page.getTotalElements(),
        Timestamp.from(clock.instant()));
    return page;
  }

  private static Specification<Receipt> specification(ReceiptCriteria c) {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      where.add(cb.equal(root.get("companyId"), c.companyId()));
      exact(where, cb, root.get("receiptNo"), c.receiptNo());
      exact(where, cb, root.get("payorCode"), c.clientCode());
      like(where, cb, root.get("payorName"), c.payor());
      like(where, cb, root.get("assuredName"), c.assured());
      facts(where, root, cb, c);
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
      ReceiptStatus status) {}
}
