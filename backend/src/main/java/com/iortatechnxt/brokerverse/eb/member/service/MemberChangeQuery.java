package com.iortatechnxt.brokerverse.eb.member.service;

import com.iortatechnxt.brokerverse.eb.domain.EbMemberChange;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberChangeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Member Changes work list (FR-EB-055): changes of a company by status (open: not closed or
 * cancelled), programme and text (change number, programme number or client name).
 */
@Service
@Transactional(readOnly = true)
public class MemberChangeQuery {

  private final EbMemberChangeRepository changes;
  private final EbProgrammeRepository programmes;

  /**
   * Creates the query.
   *
   * @param changes member changes
   * @param programmes programmes (text search)
   */
  public MemberChangeQuery(EbMemberChangeRepository changes, EbProgrammeRepository programmes) {
    this.changes = changes;
    this.programmes = programmes;
  }

  /**
   * Searches the member changes.
   *
   * @param companyId company
   * @param filter status, programme and text
   * @param pageable page
   * @return changes, latest first
   */
  public Page<EbMemberChange> search(Long companyId, Filter filter, Pageable pageable) {
    Specification<EbMemberChange> spec =
        (root, query, cb) -> {
          List<Predicate> where = new ArrayList<>();
          where.add(cb.equal(root.get("companyId"), companyId));
          if ("OPEN".equals(filter.status())) {
            where.add(
                root.get("status")
                    .in(EbMemberChange.Status.CLOSED, EbMemberChange.Status.CANCELLED)
                    .not());
          } else if (filter.status() != null && !filter.status().isBlank()) {
            where.add(cb.equal(root.get("status"), EbMemberChange.Status.valueOf(filter.status())));
          }
          if (filter.programmeId() != null) {
            where.add(cb.equal(root.get("programmeId"), filter.programmeId()));
          }
          if (filter.text() != null && !filter.text().isBlank()) {
            String like = "%" + filter.text().strip().toLowerCase(Locale.ROOT) + "%";
            Subquery<Long> programme = query.subquery(Long.class);
            var p = programme.from(EbProgramme.class);
            programme
                .select(p.get("id"))
                .where(
                    cb.or(
                        cb.like(cb.lower(p.get("programmeNo")), like),
                        cb.like(cb.lower(p.get("clientName")), like)));
            where.add(
                cb.or(
                    cb.like(cb.lower(root.get("changeNo")), like),
                    root.get("programmeId").in(programme)));
          }
          query.orderBy(cb.desc(root.get("id")));
          return cb.and(where.toArray(Predicate[]::new));
        };
    return changes.findAll(spec, pageable);
  }

  /**
   * The programme of a change (work list columns).
   *
   * @param change change
   * @return programme, null when missing
   */
  public EbProgramme programmeOf(EbMemberChange change) {
    return programmes.findById(change.getProgrammeId()).orElse(null);
  }

  /**
   * Filters of the work list.
   *
   * @param status OPEN, a status or null for all
   * @param programmeId programme, may be null
   * @param text change number, programme number or client name fragment, may be null
   */
  public record Filter(String status, Long programmeId, String text) {}
}
