package com.iortatechnxt.finverse.approval.service;

import com.iortatechnxt.finverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.finverse.common.domain.RecordStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Helper for inbox sources of maker-checker master data ({@link AuthorizableEntity}): finds the
 * records in {@link RecordStatus#PENDING_AUTHORIZATION} and maps them to inbox items, excluding the
 * viewer's own changes. The maker is the last modifier (or the creator of a new record).
 */
@Component
public class MasterRecordApprovals {

  /** Module code used for master data items. */
  public static final String MODULE = "MASTER_DATA";

  /** Permission needed to authorize master data. */
  public static final String PERMISSION = "MASTER_AUTHORIZE";

  private final EntityManager entityManager;

  /**
   * Creates the helper.
   *
   * @param entityManager entity manager
   */
  public MasterRecordApprovals(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /**
   * Pending records of one entity type as inbox items.
   *
   * @param viewer viewer
   * @param type entity class
   * @param facts maps a record to its display facts
   * @param <E> entity type
   * @return items the viewer may authorize
   */
  @Transactional(readOnly = true)
  public <E extends AuthorizableEntity> List<PendingApproval> pending(
      ApprovalViewer viewer, Class<E> type, Function<E, RecordFacts> facts) {
    if (!viewer.can(PERMISSION)) {
      return List.of();
    }
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<E> query = cb.createQuery(type);
    Root<E> root = query.from(type);
    query
        .select(root)
        .where(cb.equal(root.get("recordStatus"), RecordStatus.PENDING_AUTHORIZATION));
    return entityManager.createQuery(query).getResultList().stream()
        .filter(e -> viewer.mayApproveItemOf(maker(e)))
        .map(e -> toItem(e, facts.apply(e)))
        .toList();
  }

  private static String maker(AuthorizableEntity e) {
    return e.getUpdatedBy() != null ? e.getUpdatedBy() : e.getCreatedBy();
  }

  private static PendingApproval toItem(AuthorizableEntity e, RecordFacts f) {
    return new PendingApproval(
        MODULE,
        f.type(),
        f.reference(),
        f.description(),
        null,
        null,
        maker(e),
        e.getUpdatedAt() != null ? e.getUpdatedAt() : e.getCreatedAt(),
        f.companyId(),
        f.link());
  }

  /**
   * Display facts of a master record.
   *
   * @param type item type label, e.g. "Branch"
   * @param reference code
   * @param description name
   * @param companyId company (null for company records themselves)
   * @param link frontend route (null when no screen exists)
   */
  public record RecordFacts(
      String type, String reference, String description, Long companyId, String link) {}
}
