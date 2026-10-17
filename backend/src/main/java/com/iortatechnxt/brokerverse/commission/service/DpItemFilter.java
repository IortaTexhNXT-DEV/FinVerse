package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.DpTag;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.Sanitation;
import com.iortatechnxt.brokerverse.commission.domain.DpItem;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filters of the direct payment accounts (validation and sanitation list, CMRID.008/013).
 *
 * @param tags tags, empty for all
 * @param sanitation sanitation result, null for all
 * @param insurerCode insurer, null for all
 * @param listId list, null for all
 * @param billingId billing, null for all
 * @param text part of the invoice, policy or assured name
 */
public record DpItemFilter(
    List<DpTag> tags,
    Sanitation sanitation,
    String insurerCode,
    Long listId,
    Long billingId,
    String text) {

  /** Defensive copy. */
  public DpItemFilter {
    tags = tags == null ? List.of() : List.copyOf(tags);
  }

  /**
   * The query of the filters.
   *
   * @param companyId company
   * @return specification
   */
  Specification<DpItem> specification(Long companyId) {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      where.add(cb.equal(root.get("companyId"), companyId));
      if (!tags.isEmpty()) {
        where.add(root.get("tag").in(tags));
      }
      if (sanitation != null) {
        where.add(cb.equal(root.get("sanitation"), sanitation));
      }
      if (insurerCode != null && !insurerCode.isBlank()) {
        where.add(cb.equal(root.get("insurerCode"), insurerCode.strip()));
      }
      if (listId != null) {
        where.add(cb.equal(root.get("listId"), listId));
      }
      if (billingId != null) {
        where.add(cb.equal(root.get("billingId"), billingId));
      }
      if (text != null && !text.isBlank()) {
        String like = "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
        where.add(
            cb.or(
                cb.like(cb.lower(root.get("invoiceNo")), like),
                cb.like(cb.lower(cb.coalesce(root.get("policyNo"), "")), like),
                cb.like(cb.lower(cb.coalesce(root.get("assuredName"), "")), like)));
      }
      return cb.and(where.toArray(Predicate[]::new));
    };
  }
}
