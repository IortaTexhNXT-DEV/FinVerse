package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueryService;
import com.iortatechnxt.brokerverse.productmaint.domain.Advisory;
import com.iortatechnxt.brokerverse.productmaint.domain.AdvisoryRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.ComparativeOutputRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequestRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestScope;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestType;
import jakarta.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Package request reads for the screens (BRPM.011/019): the work list with its filters, the work
 * case facts of the listed requests (stage since, due, assignee) and the Product Maintenance home
 * counts (requests by stage with SLA overdue / due soon, packages expiring within 30 / 60 / 90
 * days, versions waiting for validation, advisories pending, comparative outputs of the week).
 */
@Service
@Transactional(readOnly = true)
public class PackageQueryService {

  private static final int[] EXPIRY_WINDOWS = {30, 60, 90};
  private static final Duration DUE_SOON = Duration.ofHours(8);
  private static final int WEEK_DAYS = 7;

  private static final String STAGE_COUNTS =
      "select r.status as stage, count(*) as total,"
          + " sum(case when c.due_at is not null and c.due_at < :now then 1 else 0 end) as overdue,"
          + " sum(case when c.due_at is not null and c.due_at >= :now and c.due_at < :soon"
          + " then 1 else 0 end) as due_soon"
          + " from pm_request r"
          + " left join wf_case c on c.entity_type = 'PackageRequest'"
          + " and c.entity_id = cast(r.id as varchar)"
          + " where r.company_id = :company group by r.status";

  private static final String CASES =
      "select c.entity_id, c.stage_entered_at, c.due_at, c.assignee from wf_case c"
          + " where c.entity_type = 'PackageRequest' and c.entity_id in (:ids)";

  private final PackageRequestRepository requests;
  private final ProductVersionQueryService versions;
  private final AdvisoryRepository advisories;
  private final ComparativeOutputRepository outputs;
  private final NamedParameterJdbcTemplate jdbc;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests package requests
   * @param versions catalog package versions
   * @param advisories advisories
   * @param outputs comparative outputs
   * @param jdbc JDBC template (work case facts)
   * @param clock clock
   */
  public PackageQueryService(
      PackageRequestRepository requests,
      ProductVersionQueryService versions,
      AdvisoryRepository advisories,
      ComparativeOutputRepository outputs,
      NamedParameterJdbcTemplate jdbc,
      Clock clock) {
    this.requests = requests;
    this.versions = versions;
    this.advisories = advisories;
    this.outputs = outputs;
    this.jdbc = jdbc;
    this.clock = clock;
  }

  /**
   * Requests matching the criteria.
   *
   * @param search criteria
   * @param pageable page and sort
   * @return page
   */
  public Page<PackageRequest> search(Search search, Pageable pageable) {
    return requests.findAll(search.toSpecification(LocalDate.now(clock)), pageable);
  }

  /**
   * Work case facts of requests (stage since, due time, assignee).
   *
   * @param ids request ids
   * @return facts by request id
   */
  public Map<Long, CaseFacts> caseFacts(List<Long> ids) {
    Map<Long, CaseFacts> facts = new HashMap<>();
    if (ids.isEmpty()) {
      return facts;
    }
    jdbc.query(
        CASES,
        new MapSqlParameterSource("ids", ids.stream().map(String::valueOf).toList()),
        rs -> {
          facts.put(
              Long.valueOf(rs.getString("entity_id")),
              new CaseFacts(
                  instant(rs.getTimestamp("stage_entered_at")),
                  instant(rs.getTimestamp("due_at")),
                  rs.getString("assignee")));
        });
    return facts;
  }

  private static Instant instant(Timestamp t) {
    return t == null ? null : t.toInstant();
  }

  /**
   * The Product Maintenance home counts (BRPM.019).
   *
   * @param companyId company
   * @return counts
   */
  public HomeCounts counts(Long companyId) {
    Instant now = clock.instant();
    List<StageCount> stages =
        jdbc.query(
            STAGE_COUNTS,
            new MapSqlParameterSource()
                .addValue("company", companyId)
                .addValue("now", Timestamp.from(now))
                .addValue("soon", Timestamp.from(now.plus(DUE_SOON))),
            (rs, i) ->
                new StageCount(
                    rs.getString("stage"),
                    rs.getLong("total"),
                    rs.getLong("overdue"),
                    rs.getLong("due_soon")));
    Map<Integer, Integer> expiring = new HashMap<>();
    for (int window : EXPIRY_WINDOWS) {
      expiring.put(window, versions.packagesExpiring(companyId, window).size());
    }
    return new HomeCounts(
        stages,
        expiring,
        advisories.countByCompanyIdAndStatus(companyId, Advisory.Status.DRAFT),
        outputs.countByCreatedAtGreaterThanEqual(now.minus(Duration.ofDays(WEEK_DAYS))));
  }

  /**
   * Criteria of the request list.
   *
   * @param companyId company
   * @param text request number, title, client or product fragment
   * @param stages stages, empty for all
   * @param types request types, empty for all
   * @param scope scope, null for both
   * @param createdBy maker, null for anyone
   * @param productCode target product, null for any
   * @param expiringWithin package end date within this many days, null for any
   */
  public record Search(
      Long companyId,
      String text,
      List<RequestStage> stages,
      List<RequestType> types,
      RequestScope scope,
      String createdBy,
      String productCode,
      Integer expiringWithin) {

    /** Defensive copies. */
    public Search {
      stages = stages == null ? List.of() : List.copyOf(stages);
      types = types == null ? List.of() : List.copyOf(types);
    }

    Specification<PackageRequest> toSpecification(LocalDate today) {
      Specification<PackageRequest> spec =
          (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
      if (!stages.isEmpty()) {
        spec = spec.and((root, query, cb) -> root.get("status").in(stages));
      }
      if (!types.isEmpty()) {
        spec = spec.and((root, query, cb) -> root.get("requestType").in(types));
      }
      if (scope != null) {
        spec = spec.and((root, query, cb) -> cb.equal(root.get("scope"), scope));
      }
      if (createdBy != null) {
        String maker = createdBy.toLowerCase(Locale.ROOT);
        spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("createdBy")), maker));
      }
      if (productCode != null) {
        spec = spec.and((root, query, cb) -> cb.equal(root.get("targetProductCode"), productCode));
      }
      if (expiringWithin != null) {
        LocalDate until = today.plusDays(expiringWithin);
        spec =
            spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("packageEndDate"), until));
      }
      return text == null || text.isBlank() ? spec : spec.and(matching(text));
    }

    private static Specification<PackageRequest> matching(String fragment) {
      String pattern = "%" + fragment.strip().toLowerCase(Locale.ROOT) + "%";
      List<String> fields =
          List.of("requestNo", "title", "clientCode", "clientName", "targetProductCode");
      return (root, query, cb) ->
          cb.or(
              fields.stream()
                  .map(f -> cb.like(cb.lower(cb.coalesce(root.<String>get(f), "")), pattern))
                  .toArray(Predicate[]::new));
    }
  }

  /**
   * Work case facts of a request.
   *
   * @param stageEnteredAt when the current stage started
   * @param dueAt SLA due time, null when none
   * @param assignee assignee, null when unassigned
   */
  public record CaseFacts(Instant stageEnteredAt, Instant dueAt, String assignee) {}

  /**
   * Requests in one stage.
   *
   * @param stage stage
   * @param total requests
   * @param overdue past their SLA
   * @param dueSoon due within eight hours
   */
  public record StageCount(String stage, long total, long overdue, long dueSoon) {}

  /**
   * Product Maintenance home counts.
   *
   * @param stages requests by stage
   * @param expiring packages expiring within 30, 60 and 90 days
   * @param advisoriesPending draft advisories
   * @param outputsThisWeek comparative outputs generated in the last seven days
   */
  public record HomeCounts(
      List<StageCount> stages,
      Map<Integer, Integer> expiring,
      long advisoriesPending,
      long outputsThisWeek) {

    /** Defensive copies. */
    public HomeCounts {
      stages = List.copyOf(stages);
      expiring = Map.copyOf(expiring);
    }
  }
}
