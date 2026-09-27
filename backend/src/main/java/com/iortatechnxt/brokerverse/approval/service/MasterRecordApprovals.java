package com.iortatechnxt.brokerverse.approval.service;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
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
    return pending(viewer, new Scope(MODULE, PERMISSION), type, facts);
  }

  /**
   * Pending records of one entity type as inbox items, for records that a business module
   * authorizes under its own permission (e.g. products under {@code POLICY_AUTHORIZE}).
   *
   * @param viewer viewer
   * @param scope module code shown in the inbox and permission needed to authorize the records
   * @param type entity class
   * @param facts maps a record to its display facts
   * @param <E> entity type
   * @return items the viewer may authorize
   */
  @Transactional(readOnly = true)
  public <E extends AuthorizableEntity> List<PendingApproval> pending(
      ApprovalViewer viewer, Scope scope, Class<E> type, Function<E, RecordFacts> facts) {
    if (!viewer.can(scope.permission())) {
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
        .map(e -> toItem(scope.module(), e, facts.apply(e)))
        .toList();
  }

  private static String maker(AuthorizableEntity e) {
    return e.getUpdatedBy() != null ? e.getUpdatedBy() : e.getCreatedBy();
  }

  private static PendingApproval toItem(String module, AuthorizableEntity e, RecordFacts f) {
    return new PendingApproval(
        module,
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

  /**
   * Who authorizes a kind of master record.
   *
   * @param module module code shown in the inbox, e.g. "UNDERWRITING"
   * @param permission permission the controller requires to authorize the record
   */
  public record Scope(String module, String permission) {}
}
