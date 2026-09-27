package com.iortatechnxt.brokerverse.migration.archive.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.migration.archive.domain.AccessLog;
import com.iortatechnxt.brokerverse.migration.archive.domain.ArchiveRecord;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** The queries of the Legacy Inquiry and of the access log. */
final class ArchiveSpecifications {

  private static final String COMPANY = "companyId";

  private ArchiveSpecifications() {}

  static Specification<ArchiveRecord> matching(Long companyId, ArchiveCriteria c) {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      where.add(cb.equal(root.get(COMPANY), companyId));
      where.add(cb.isFalse(root.get("rolledBack")));
      if (present(c.client())) {
        String text = c.client().strip();
        where.add(
            cb.or(
                cb.equal(root.get("clientKey"), text),
                cb.like(
                    cb.lower(root.get("clientName")), "%" + text.toLowerCase(Locale.ROOT) + "%")));
      }
      equal(where, cb, root, "policyNo", c.policyNo());
      equal(where, cb, root, "invoiceNo", c.invoiceNo());
      equal(where, cb, root, "receiptNo", c.receiptNo());
      equal(where, cb, root, "claimNo", c.claimNo());
      equal(where, cb, root, "recordType", upper(c.recordType()));
      equal(where, cb, root, "sourceSystem", upper(c.sourceSystem()));
      if (c.from() != null) {
        where.add(cb.greaterThanOrEqualTo(root.get("documentDate"), c.from()));
      }
      if (c.to() != null) {
        where.add(cb.lessThanOrEqualTo(root.get("documentDate"), c.to()));
      }
      return cb.and(where.toArray(Predicate[]::new));
    };
  }

  static Specification<AccessLog> log(Long companyId, AccessLogFilter f) {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      where.add(cb.equal(root.get(COMPANY), companyId));
      if (present(f.username())) {
        where.add(cb.equal(root.get("username"), f.username().strip()));
      }
      if (present(f.action())) {
        where.add(cb.equal(root.get("action"), upper(f.action())));
      }
      if (f.from() != null) {
        where.add(cb.greaterThanOrEqualTo(root.get("accessedAt"), start(f.from())));
      }
      if (f.to() != null) {
        where.add(cb.lessThan(root.get("accessedAt"), start(f.to().plusDays(1))));
      }
      return cb.and(where.toArray(Predicate[]::new));
    };
  }

  private static Instant start(LocalDate date) {
    return date.atStartOfDay(BusinessClock.zone()).toInstant();
  }

  private static void equal(
      List<Predicate> where, CriteriaBuilder cb, Root<ArchiveRecord> root, String field, String v) {
    if (present(v)) {
      where.add(cb.equal(root.get(field), v.strip()));
    }
  }

  private static boolean present(String value) {
    return value != null && !value.isBlank();
  }

  private static String upper(String value) {
    return present(value) ? value.strip().toUpperCase(Locale.ROOT) : null;
  }
}
