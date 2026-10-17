package com.iortatechnxt.brokerverse.audit.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.domain.AuditLog;
import com.iortatechnxt.brokerverse.audit.domain.AuditLogRepository;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Audit Trail as BDOI's FRS shows it (FRUM.008.02 and FRPM.021.01): filters by date range,
 * user, record type, reference number and action; sortable columns; for each entry the timestamp,
 * module, user ID (Windows ID), performed by, role, action, activity, from and to values and the
 * source address.
 */
@Service
@Transactional(readOnly = true)
public class AuditTrailQuery {

  /** Columns the list can be sorted by. */
  public static final Set<String> SORTABLE =
      Set.of("occurredAt", "username", "action", "entityType", "entityId");

  /** Most rows of an export. */
  public static final int MAX_EXPORT_ROWS = 20_000;

  private static final int MAX_PAGE_SIZE = 200;
  private static final String OCCURRED_AT = "occurredAt";
  private static final Map<AuditAction, String> ACTION_LABELS = new EnumMap<>(AuditAction.class);

  static {
    ACTION_LABELS.put(AuditAction.CREATE, "Create");
    ACTION_LABELS.put(AuditAction.UPDATE, "Update");
    ACTION_LABELS.put(AuditAction.AUTHORIZE, "Approve");
    ACTION_LABELS.put(AuditAction.REJECT, "Reject");
    ACTION_LABELS.put(AuditAction.DEACTIVATE, "Deactivate");
    ACTION_LABELS.put(AuditAction.SUBMIT, "Submit");
    ACTION_LABELS.put(AuditAction.POST, "Post");
    ACTION_LABELS.put(AuditAction.REVERSE, "Reverse");
    ACTION_LABELS.put(AuditAction.OPEN, "Open");
    ACTION_LABELS.put(AuditAction.CLOSE, "Close");
    ACTION_LABELS.put(AuditAction.REOPEN, "Reopen");
    ACTION_LABELS.put(AuditAction.RUN, "Generate");
    ACTION_LABELS.put(AuditAction.LOGIN, "Login");
    ACTION_LABELS.put(AuditAction.LOGIN_FAILED, "Failed Login");
    ACTION_LABELS.put(AuditAction.LOGOUT, "Logout");
    ACTION_LABELS.put(AuditAction.INACTIVITY, "Inactivity");
    ACTION_LABELS.put(AuditAction.TIMEOUT, "Timeout");
    ACTION_LABELS.put(AuditAction.EXPORT, "Export");
  }

  private static final String KEY = "|";

  private final AuditLogRepository repository;
  private final ObjectProvider<ActorRoles> directory;
  private final ObjectProvider<AuditSubjects> subjects;

  /**
   * Creates the query.
   *
   * @param repository audit trail
   * @param directory Windows IDs of the users
   * @param subjects client or assured's names of the audited records
   */
  public AuditTrailQuery(
      AuditLogRepository repository,
      ObjectProvider<ActorRoles> directory,
      ObjectProvider<AuditSubjects> subjects) {
    this.repository = repository;
    this.directory = directory;
    this.subjects = subjects;
  }

  /**
   * BDOI's word for an action.
   *
   * @param action action
   * @return label
   */
  public static String label(AuditAction action) {
    return ACTION_LABELS.getOrDefault(action, AuditModules.words(action.name()));
  }

  /**
   * Searches the audit trail.
   *
   * @param filter the filters
   * @param page page index
   * @param size page size
   * @return entries of the page
   */
  public Page<AuditEntry> search(Filter filter, int page, int size) {
    return find(filter, page, Math.min(size, MAX_PAGE_SIZE));
  }

  /**
   * Every entry of the filters, for an export (at most {@value #MAX_EXPORT_ROWS}).
   *
   * @param filter the filters
   * @return entries
   */
  public List<AuditEntry> all(Filter filter) {
    return find(filter, 0, MAX_EXPORT_ROWS).getContent();
  }

  private Page<AuditEntry> find(Filter filter, int page, int size) {
    Sort sort =
        Sort.by(
            "asc".equals(filter.direction()) ? Sort.Direction.ASC : Sort.Direction.DESC,
            filter.sort() != null && SORTABLE.contains(filter.sort())
                ? filter.sort()
                : OCCURRED_AT);
    Page<AuditLog> logs =
        repository.findAll(
            specification(filter), PageRequest.of(page, size, sort.and(Sort.by("id"))));
    Map<String, String> windowsIds =
        windowsIds(logs.getContent().stream().map(AuditLog::getUsername).toList());
    Map<String, String> names = subjects(logs.getContent());
    return logs.map(
        a ->
            new AuditEntry(
                a,
                AuditModules.of(a.getEntityType()),
                windowsIds.get(a.getUsername().toLowerCase(Locale.ROOT)),
                label(a.getAction()),
                names.get(a.getEntityType() + KEY + a.getEntityId())));
  }

  private Map<String, String> subjects(List<AuditLog> logs) {
    Map<String, String> found = new HashMap<>();
    logs.stream()
        .collect(
            Collectors.groupingBy(
                AuditLog::getEntityType,
                Collectors.mapping(AuditLog::getEntityId, Collectors.toSet())))
        .forEach(
            (type, ids) ->
                subjects
                    .orderedStream()
                    .forEach(
                        s ->
                            s.subjects(type, ids)
                                .forEach((id, name) -> found.putIfAbsent(type + KEY + id, name))));
    return found;
  }

  private static Specification<AuditLog> specification(Filter filter) {
    Instant start = BusinessClock.startOf(filter.from());
    Instant end = BusinessClock.startOf(filter.to().plusDays(1));
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      where.add(cb.greaterThanOrEqualTo(root.get(OCCURRED_AT), start));
      where.add(cb.lessThan(root.get(OCCURRED_AT), end));
      if (filter.username() != null) {
        where.add(
            cb.equal(cb.lower(root.get("username")), filter.username().toLowerCase(Locale.ROOT)));
      }
      if (!filter.entityTypes().isEmpty()) {
        where.add(root.get("entityType").in(filter.entityTypes()));
      }
      if (filter.reference() != null) {
        where.add(
            cb.like(
                cb.lower(root.get("entityId")),
                "%" + filter.reference().toLowerCase(Locale.ROOT) + "%"));
      }
      if (filter.action() != null) {
        where.add(cb.equal(root.get("action"), filter.action()));
      }
      return cb.and(where.toArray(Predicate[]::new));
    };
  }

  private Map<String, String> windowsIds(Collection<String> usernames) {
    ActorRoles lookup = directory.getIfAvailable();
    return lookup == null ? Map.of() : lookup.windowsIds(usernames);
  }

  /**
   * Filters of the search.
   *
   * @param from first date
   * @param to last date
   * @param username user, null for all
   * @param entityTypes record types, empty for all
   * @param reference part of the reference number, null for all
   * @param action action, null for all
   * @param sort column to sort by (see {@link #SORTABLE}), newest first by default
   * @param direction asc or desc
   */
  public record Filter(
      LocalDate from,
      LocalDate to,
      String username,
      List<String> entityTypes,
      String reference,
      AuditAction action,
      String sort,
      String direction) {

    /** Defensive copy. */
    public Filter {
      entityTypes = entityTypes == null ? List.of() : List.copyOf(entityTypes);
    }
  }

  /**
   * An entry with the module, the Windows ID of the user and the action in words.
   *
   * @param log the entry
   * @param module module
   * @param windowsId Windows ID of the user, null when none
   * @param actionLabel action in BDOI's words
   * @param subject client or assured's name of the record, null when none
   */
  public record AuditEntry(
      AuditLog log, String module, String windowsId, String actionLabel, String subject) {}
}
