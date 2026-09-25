package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycle;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.ReconStatus;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.UnbookedStatus;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItem;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItem.Feedback;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItemRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The reconciliation workbench (PRCID.012-016/018/021/030/033): items of a cycle by status bucket
 * with the AO / AB, segment and product filters, the handler's feedback and disposition per item or
 * for a selection (company concerned and disposition from their lists of values, OQ31), and the
 * unbooked repository.
 */
@Service
@Transactional
public class ReconItemService {

  /** List of the company concerned (PRCID.016). */
  public static final String COMPANY_CONCERNED = "RECON_COMPANY_CONCERNED";

  /** List of dispositions (PRCID.038/039). */
  public static final String DISPOSITION = "RECON_DISPOSITION";

  private static final String ENTITY = "ReconItem";

  private final ReconItemRepository items;
  private final ReconCycleService cycles;
  private final LovService lovs;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param items items
   * @param cycles cycles
   * @param lovs lists of values
   * @param audit audit trail
   * @param clock clock
   */
  public ReconItemService(
      ReconItemRepository items,
      ReconCycleService cycles,
      LovService lovs,
      AuditTrailService audit,
      Clock clock) {
    this.items = items;
    this.cycles = cycles;
    this.lovs = lovs;
    this.audit = audit;
    this.clock = clock;
  }

  /** Status buckets of the workbench tabs. */
  public enum Bucket {
    /** Every item. */
    ALL(List.of(ReconStatus.values())),
    /** Matched. */
    MATCHED(List.of(ReconStatus.MATCHED)),
    /** Matched with discrepancies. */
    DISCREPANCY(List.of(ReconStatus.MATCHED_WITH_DISCREPANCY)),
    /** Booked by BDOI only. */
    BDOI_ONLY(List.of(ReconStatus.BDOI_ONLY)),
    /** Insurer production without a booked invoice. */
    INSURER_ONLY(List.of(ReconStatus.UNMATCHED_PREBOOKED, ReconStatus.UNMATCHED_NO_BOOKING));

    private final List<ReconStatus> statuses;

    Bucket(List<ReconStatus> statuses) {
      this.statuses = statuses;
    }

    /**
     * Statuses of the bucket.
     *
     * @return statuses
     */
    public List<ReconStatus> statuses() {
      return statuses;
    }
  }

  /**
   * Items of a cycle (PRCID.012-014/021/030).
   *
   * @param cycleId cycle
   * @param filter bucket and filters
   * @param pageable page
   * @return items
   */
  @Transactional(readOnly = true)
  public Page<ReconItem> items(Long cycleId, ItemFilter filter, Pageable pageable) {
    cycles.require(cycleId);
    return items.findAll(specification(cycleId, filter), pageable);
  }

  /**
   * An item.
   *
   * @param id item
   * @return item
   */
  @Transactional(readOnly = true)
  public ReconItem require(Long id) {
    return items.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Records feedback and disposition on an item (PRCID.015/016/018).
   *
   * @param id item
   * @param feedback feedback
   * @return the item
   */
  public ReconItem feedback(Long id, Feedback feedback) {
    ReconItem item = require(id);
    ReconCycle cycle = cycles.requireOpen(item.getCycleId());
    validate(feedback);
    item.feedback(feedback);
    audit.record(ENTITY, id, AuditAction.UPDATE, describe(feedback));
    cycles.closeWhenSettled(cycle);
    return item;
  }

  /**
   * Sets company concerned, disposition and closure on a selection of items; other feedback is
   * kept.
   *
   * @param ids items
   * @param change the values to set
   * @return the items
   */
  public List<ReconItem> bulkFeedback(List<Long> ids, BulkChange change) {
    List<ReconItem> changed = new ArrayList<>();
    ReconCycle cycle = null;
    for (Long id : ids) {
      ReconItem item = require(id);
      cycle = cycles.requireOpen(item.getCycleId());
      Feedback current = item.currentFeedback();
      Feedback next =
          new Feedback(
              change.companyConcerned() == null
                  ? current.companyConcerned()
                  : change.companyConcerned(),
              current.instruction(),
              current.insurerFeedback(),
              current.marketingFeedback(),
              change.disposition() == null ? current.disposition() : change.disposition(),
              change.forClosure());
      validate(next);
      item.feedback(next);
      audit.record(ENTITY, id, AuditAction.UPDATE, "Bulk: " + describe(next));
      changed.add(item);
    }
    if (cycle != null) {
      cycles.closeWhenSettled(cycle);
    }
    return changed;
  }

  private void validate(Feedback f) {
    LocalDate today = LocalDate.now(clock);
    lovs.validateOptional(COMPANY_CONCERNED, blankToNull(f.companyConcerned()), today);
    lovs.validateOptional(DISPOSITION, blankToNull(f.disposition()), today);
  }

  private static String blankToNull(String v) {
    return v == null || v.isBlank() ? null : v;
  }

  private static String describe(Feedback f) {
    return "Company concerned "
        + f.companyConcerned()
        + ", disposition "
        + f.disposition()
        + (f.forClosure() ? ", for closure" : "");
  }

  /**
   * The unbooked repository (PRCID.019/033).
   *
   * @param companyId company
   * @param insurer insurer, null for all
   * @param status resolution, null for all
   * @param text search text, null for all
   * @param pageable page
   * @return insurer-only items
   */
  @Transactional(readOnly = true)
  public Page<ReconItem> unbooked(
      Long companyId, String insurer, UnbookedStatus status, String text, Pageable pageable) {
    String like = text == null || text.isBlank() ? null : text.strip().toLowerCase(Locale.ROOT);
    return items.unbooked(companyId, blankToNull(insurer), status, like, pageable);
  }

  private static Specification<ReconItem> specification(Long cycleId, ItemFilter f) {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      where.add(cb.equal(root.get("cycleId"), cycleId));
      where.add(root.get("status").in(f.bucket().statuses()));
      equalsIfSet(where, cb, root, "aoUsername", f.aoUsername());
      equalsIfSet(where, cb, root, "salesUnit", f.salesUnit());
      equalsIfSet(where, cb, root, "segment", f.segment());
      equalsIfSet(where, cb, root, "productLine", f.productLine());
      if (f.text() != null && !f.text().isBlank()) {
        String like = "%" + f.text().strip().toLowerCase(Locale.ROOT) + "%";
        where.add(
            cb.or(
                cb.like(cb.lower(cb.coalesce(root.get("invoiceNo"), "")), like),
                cb.like(cb.lower(cb.coalesce(root.get("bdoi").get("policyNo"), "")), like),
                cb.like(cb.lower(cb.coalesce(root.get("insurer").get("policyNo"), "")), like),
                cb.like(cb.lower(cb.coalesce(root.get("insurer").get("referenceNo"), "")), like),
                cb.like(cb.lower(cb.coalesce(root.get("bdoi").get("assuredName"), "")), like),
                cb.like(cb.lower(cb.coalesce(root.get("insurer").get("assuredName"), "")), like)));
      }
      return cb.and(where.toArray(Predicate[]::new));
    };
  }

  private static void equalsIfSet(
      List<Predicate> where, CriteriaBuilder cb, Root<ReconItem> root, String field, String v) {
    if (v != null && !v.isBlank()) {
      where.add(cb.equal(root.get(field), v.strip()));
    }
  }

  /**
   * Workbench filters (PRCID.013/014/021).
   *
   * @param bucket status bucket
   * @param text part of the invoice, policy or assured name
   * @param aoUsername account officer
   * @param salesUnit sales unit / AB
   * @param segment market segment
   * @param productLine product line
   */
  public record ItemFilter(
      Bucket bucket,
      String text,
      String aoUsername,
      String salesUnit,
      String segment,
      String productLine) {

    /** Defaults to every bucket. */
    public ItemFilter {
      bucket = bucket == null ? Bucket.ALL : bucket;
    }
  }

  /**
   * A change applied to a selection.
   *
   * @param companyConcerned company concerned, null to keep
   * @param disposition disposition, null to keep
   * @param forClosure confirmed for closure
   */
  public record BulkChange(String companyConcerned, String disposition, boolean forClosure) {}
}
