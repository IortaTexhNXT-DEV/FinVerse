package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Criteria of the quotation list.
 *
 * @param companyId company
 * @param text quotation number, ARN, client code or name fragment
 * @param statuses statuses, empty for all
 * @param productCode product
 * @param createdBy maker (My drafts), may be null
 * @param validUntilOnOrBefore expiring on or before this date, may be null
 * @param clientId client, may be null
 */
public record QuotationSearch(
    Long companyId,
    String text,
    List<QuotationStatus> statuses,
    String productCode,
    String createdBy,
    LocalDate validUntilOnOrBefore,
    Long clientId) {

  /** Defensive copy. */
  public QuotationSearch {
    statuses = statuses == null ? List.of() : List.copyOf(statuses);
  }

  /**
   * As a JPA specification.
   *
   * @return specification
   */
  public Specification<Quotation> toSpecification() {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      where.add(cb.equal(root.get("companyId"), companyId));
      if (!statuses.isEmpty()) {
        where.add(root.get("status").in(statuses));
      }
      if (text != null && !text.isBlank()) {
        String like = "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
        where.add(
            cb.or(
                cb.like(cb.lower(root.get("quotationNo")), like),
                cb.like(cb.lower(root.get("arn")), like),
                cb.like(cb.lower(root.get("clientCode")), like),
                cb.like(cb.lower(root.get("clientName")), like)));
      }
      if (productCode != null && !productCode.isBlank()) {
        where.add(cb.equal(root.get("productCode"), productCode.strip()));
      }
      if (createdBy != null) {
        where.add(cb.equal(cb.lower(root.get("createdBy")), createdBy.toLowerCase(Locale.ROOT)));
      }
      if (validUntilOnOrBefore != null) {
        where.add(cb.lessThanOrEqualTo(root.get("validUntil"), validUntilOnOrBefore));
      }
      if (clientId != null) {
        where.add(cb.equal(root.get("clientId"), clientId));
      }
      return cb.and(where.toArray(Predicate[]::new));
    };
  }
}
