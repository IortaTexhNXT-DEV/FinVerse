package com.iortatechnxt.brokerverse.csf.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.csf.domain.ChangeStatus;
import com.iortatechnxt.brokerverse.csf.domain.CsfContactChange;
import com.iortatechnxt.brokerverse.csf.domain.CsfContactChangeRepository;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.ChangeView;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Contact Changes list (FR-CSF-021, 022, 041): contact changes, refusals and referrals of a
 * company with their verification, sync status and referral state, for the supervisors and the
 * agents' follow-up.
 */
@Service
@Transactional(readOnly = true)
public class ChangeQueryService {

  private static final String APPLIED_AT = "appliedAt";

  private final CsfContactChangeRepository changes;
  private final ChangeViews views;

  /**
   * Creates the service.
   *
   * @param changes contact changes
   * @param views change views
   */
  public ChangeQueryService(CsfContactChangeRepository changes, ChangeViews views) {
    this.changes = changes;
    this.views = views;
  }

  /**
   * One page of changes, newest first.
   *
   * @param filter criteria
   * @param pageable page
   * @return changes
   */
  public Page<ChangeView> list(ChangeFilter filter, Pageable pageable) {
    Page<CsfContactChange> page =
        changes.findAll(
            specification(filter),
            PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, APPLIED_AT, "id")));
    List<Long> ids = page.getContent().stream().map(CsfContactChange::getId).toList();
    Map<Long, CsfContactChange> loaded =
        changes.findByIdIn(ids).stream()
            .collect(Collectors.toMap(CsfContactChange::getId, Function.identity()));
    List<ChangeView> content = views.of(ids.stream().map(loaded::get).toList());
    return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
  }

  private static Specification<CsfContactChange> specification(ChangeFilter f) {
    return (root, query, cb) -> {
      List<Predicate> p = new ArrayList<>();
      p.add(cb.equal(root.get("companyId"), f.companyId()));
      if (f.status() != null) {
        p.add(cb.equal(root.get("status"), f.status()));
      }
      if (f.agent() != null && !f.agent().isBlank()) {
        p.add(cb.equal(cb.lower(root.get("agent")), f.agent().strip().toLowerCase(Locale.ROOT)));
      }
      if (f.from() != null) {
        p.add(
            cb.greaterThanOrEqualTo(
                root.<Instant>get(APPLIED_AT), BusinessClock.startOf(f.from())));
      }
      if (f.to() != null) {
        p.add(
            cb.lessThan(root.<Instant>get(APPLIED_AT), BusinessClock.startOf(f.to().plusDays(1))));
      }
      if (f.text() != null && !f.text().isBlank()) {
        String like = "%" + f.text().strip().toLowerCase(Locale.ROOT) + "%";
        p.add(
            cb.or(
                cb.like(cb.lower(root.get("changeNo")), like),
                cb.like(cb.lower(root.get("clientCode")), like),
                cb.like(cb.lower(root.get("clientName")), like)));
      }
      return cb.and(p.toArray(Predicate[]::new));
    };
  }

  /**
   * Criteria of the list; null criteria are ignored.
   *
   * @param companyId company
   * @param status state
   * @param agent agent user
   * @param from first day
   * @param to last day
   * @param text change number, client code or name fragment
   */
  public record ChangeFilter(
      Long companyId,
      ChangeStatus status,
      String agent,
      LocalDate from,
      LocalDate to,
      String text) {}
}
