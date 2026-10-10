package com.iortatechnxt.brokerverse.identity.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEvent;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventRepository;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The provisioning events for the monitoring screen, newest first. */
@Service
@Transactional(readOnly = true)
public class IdentityEventQuery {

  private static final int MAX_PAGE = 100;

  private final IdentityEventRepository events;

  /**
   * Creates the query.
   *
   * @param events events
   */
  public IdentityEventQuery(IdentityEventRepository events) {
    this.events = events;
  }

  /**
   * Searches the events.
   *
   * @param filter filters
   * @param page page index
   * @param size page size
   * @return events of the page
   */
  public Page<IdentityEvent> search(Filter filter, int page, int size) {
    return events.findAll(
        specification(filter),
        PageRequest.of(
            page, Math.min(Math.max(size, 1), MAX_PAGE), Sort.by(Sort.Direction.DESC, "id")));
  }

  private static Specification<IdentityEvent> specification(Filter f) {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      if (f.status() != null) {
        where.add(cb.equal(root.get("status"), f.status()));
      }
      if (f.windowsId() != null && !f.windowsId().isBlank()) {
        where.add(
            cb.like(
                cb.lower(root.get("windowsId")),
                "%" + f.windowsId().trim().toLowerCase(Locale.ROOT) + "%"));
      }
      if (f.from() != null) {
        where.add(cb.greaterThanOrEqualTo(root.get("receivedAt"), BusinessClock.startOf(f.from())));
      }
      if (f.to() != null) {
        where.add(cb.lessThan(root.get("receivedAt"), BusinessClock.startOf(f.to().plusDays(1))));
      }
      return cb.and(where.toArray(Predicate[]::new));
    };
  }

  /**
   * Filters of the events.
   *
   * @param status outcome, null for all
   * @param windowsId part of the Windows ID
   * @param from first date received
   * @param to last date received
   */
  public record Filter(
      IdentityEventStatus status, String windowsId, LocalDate from, LocalDate to) {}
}
