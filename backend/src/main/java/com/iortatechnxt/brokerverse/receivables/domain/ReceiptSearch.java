package com.iortatechnxt.brokerverse.receivables.domain;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Receipt search criteria; null values are ignored.
 *
 * @param companyId company (mandatory)
 * @param status status
 * @param partyCode payer party code
 * @param mode payment instrument
 * @param from receipt date from
 * @param to receipt date to
 * @param receiptNo receipt number (contains, case insensitive)
 */
public record ReceiptSearch(
    Long companyId,
    ReceiptStatus status,
    String partyCode,
    ReceiptMode mode,
    LocalDate from,
    LocalDate to,
    String receiptNo) {

  /**
   * Builds the JPA specification.
   *
   * @return specification
   */
  public Specification<Receipt> toSpecification() {
    return (root, query, cb) -> {
      List<Predicate> p = new ArrayList<>();
      p.add(cb.equal(root.get("companyId"), companyId));
      if (status != null) {
        p.add(cb.equal(root.get("status"), status));
      }
      if (partyCode != null && !partyCode.isBlank()) {
        p.add(cb.equal(root.get("partyCode"), partyCode.trim()));
      }
      if (mode != null) {
        p.add(cb.equal(root.get("mode"), mode));
      }
      if (from != null) {
        p.add(cb.greaterThanOrEqualTo(root.get("receiptDate"), from));
      }
      if (to != null) {
        p.add(cb.lessThanOrEqualTo(root.get("receiptDate"), to));
      }
      if (receiptNo != null && !receiptNo.isBlank()) {
        p.add(
            cb.like(
                cb.lower(root.get("receiptNo")),
                "%" + receiptNo.trim().toLowerCase(Locale.ROOT) + "%"));
      }
      return cb.and(p.toArray(new Predicate[0]));
    };
  }
}
