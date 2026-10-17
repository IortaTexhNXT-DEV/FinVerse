package com.iortatechnxt.brokerverse.collections.worklist.service;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.ItemStatus;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItemRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads of the PR worklist (BRCLXN.001-012, 003): the paged, filtered list; totals grouped by
 * client or account (ARN) for the same filters; and the open amounts per segment and aging bracket
 * of the Collections home.
 */
@Service
@Transactional(readOnly = true)
public class WorklistQueryService {

  private static final int MAX_GROUPS = 200;

  private final CollectionItemRepository items;
  private final EntityManager em;

  /**
   * Creates the service.
   *
   * @param items items
   * @param em entity manager (aggregates)
   */
  public WorklistQueryService(CollectionItemRepository items, EntityManager em) {
    this.items = items;
    this.em = em;
  }

  /**
   * The worklist page.
   *
   * @param companyId company
   * @param filter filters
   * @param pageable page and sort
   * @return items
   */
  public Page<CollectionItem> search(Long companyId, WorklistFilter filter, Pageable pageable) {
    return items.findAll(WorklistSpecifications.of(companyId, filter), pageable);
  }

  /**
   * Counts the items of a filter.
   *
   * @param companyId company
   * @param filter filters
   * @return count
   */
  public long count(Long companyId, WorklistFilter filter) {
    return items.count(WorklistSpecifications.of(companyId, filter));
  }

  /**
   * Totals per client or per account for the filters (BRCLXN.003), largest first.
   *
   * @param companyId company
   * @param filter filters
   * @param groupBy CLIENT or ARN
   * @return up to 200 groups
   */
  public List<GroupTotal> totals(Long companyId, WorklistFilter filter, GroupBy groupBy) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Tuple> q = cb.createTupleQuery();
    Root<CollectionItem> root = q.from(CollectionItem.class);
    Path<String> key =
        root.get(WorklistSpecifications.PARTIES)
            .get(groupBy == GroupBy.CLIENT ? "clientCode" : "arn");
    Expression<BigDecimal> net =
        cb.sum(
            root.get(WorklistSpecifications.FIGURES)
                .<BigDecimal>get(WorklistSpecifications.NET_OUTSTANDING));
    Expression<String> name =
        cb.least(root.get(WorklistSpecifications.PARTIES).<String>get("assuredName"));
    Expression<Long> count = cb.count(root);
    q.multiselect(key, name, count, net)
        .where(WorklistSpecifications.of(companyId, filter).toPredicate(root, q, cb))
        .groupBy(key)
        .orderBy(cb.desc(net));
    return em.createQuery(q).setMaxResults(MAX_GROUPS).getResultList().stream()
        .map(t -> new GroupTotal(t.get(key), t.get(name), t.get(count), t.get(net)))
        .toList();
  }

  /**
   * Open items and amounts per segment and aging bracket (Collections home chart).
   *
   * @param companyId company
   * @return cells
   */
  public List<AgingCell> aging(Long companyId) {
    return em
        .createQuery(
            "select i.classification.segment as segment, i.figures.agingBracket as bracket,"
                + " count(i) as items, sum(i.figures.netOutstanding) as net"
                + " from CollectionItem i where i.companyId = :companyId and i.status = :status"
                + " group by i.classification.segment, i.figures.agingBracket",
            Tuple.class)
        .setParameter("companyId", companyId)
        .setParameter("status", ItemStatus.OPEN)
        .getResultList()
        .stream()
        .map(
            t ->
                new AgingCell(
                    t.get("segment", String.class),
                    t.get("bracket", String.class),
                    t.get("items", Long.class),
                    t.get("net", BigDecimal.class)))
        .toList();
  }

  /** Grouping of the totals. */
  public enum GroupBy {
    /** Per client. */
    CLIENT,
    /** Per account (ARN). */
    ARN
  }

  /**
   * Total of a group.
   *
   * @param key client code or ARN
   * @param name an assured name of the group
   * @param items items
   * @param netOutstanding sum of the net outstanding
   */
  public record GroupTotal(String key, String name, long items, BigDecimal netOutstanding) {}

  /**
   * Open items of a segment in a bracket.
   *
   * @param segment segment, null when not classified
   * @param bracket aging bracket
   * @param items items
   * @param netOutstanding sum of the net outstanding
   */
  public record AgingCell(String segment, String bracket, long items, BigDecimal netOutstanding) {}
}
