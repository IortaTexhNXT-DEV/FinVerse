package com.iortatechnxt.brokerverse.messaging.service;

import com.iortatechnxt.brokerverse.messaging.domain.MessageStatus;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Outbox filters (all optional).
 *
 * @param status status
 * @param purpose purpose code
 * @param text recipient, subject or reference contains
 */
public record MessageSearch(MessageStatus status, String purpose, String text) {

  Specification<OutboundMessage> toSpecification() {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      if (status != null) {
        where.add(cb.equal(root.get("status"), status));
      }
      if (purpose != null && !purpose.isBlank()) {
        where.add(cb.equal(root.get("purpose"), purpose));
      }
      if (text != null && !text.isBlank()) {
        String like = "%" + text.toLowerCase(Locale.ROOT) + "%";
        where.add(
            cb.or(
                cb.like(cb.lower(root.get("recipients")), like),
                cb.like(cb.lower(root.get("subject")), like),
                cb.like(cb.lower(root.get("reference")), like)));
      }
      if (query != null) {
        query.orderBy(cb.desc(root.get("id")));
      }
      return cb.and(where.toArray(Predicate[]::new));
    };
  }
}
