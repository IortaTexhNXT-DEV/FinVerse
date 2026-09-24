package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Shared queries of the {@link RetentionCandidateProvider} implementations (BRNB.106): the rule's
 * statuses that exist in the record's status enum, and the records in those statuses whose last
 * change (update, else creation) is on or before the cutoff, oldest first. The entity must have a
 * {@code status} attribute of the enum type.
 */
public final class RetentionQueries {

  private static final String STATUS = "status";
  private static final String UPDATED_AT = "updatedAt";
  private static final String CREATED_AT = "createdAt";

  private RetentionQueries() {}

  /**
   * Number of eligible records.
   *
   * @param repository repository of the record type
   * @param criteria rule statuses and cutoff
   * @param statusType status enum of the record type
   * @param <T> entity type
   * @param <E> status enum
   * @return count; 0 when no rule status exists in the enum
   */
  public static <T extends BaseEntity, E extends Enum<E>> long count(
      JpaSpecificationExecutor<T> repository, RetentionCriteria criteria, Class<E> statusType) {
    Set<E> statuses = statuses(criteria, statusType);
    return statuses.isEmpty()
        ? 0
        : repository.count(eligible(statuses, criteria.lastActivityOnOrBefore()));
  }

  /**
   * Eligible records, oldest activity first.
   *
   * @param repository repository of the record type
   * @param criteria rule statuses and cutoff
   * @param statusType status enum of the record type
   * @param limit maximum number of records
   * @param <T> entity type
   * @param <E> status enum
   * @return records
   */
  public static <T extends BaseEntity, E extends Enum<E>> List<T> oldestFirst(
      JpaSpecificationExecutor<T> repository,
      RetentionCriteria criteria,
      Class<E> statusType,
      int limit) {
    Set<E> statuses = statuses(criteria, statusType);
    if (statuses.isEmpty()) {
      return List.of();
    }
    return repository
        .findAll(
            eligible(statuses, criteria.lastActivityOnOrBefore()),
            PageRequest.of(0, limit, Sort.by(UPDATED_AT, CREATED_AT)))
        .getContent();
  }

  /**
   * Date of the last change of a record.
   *
   * @param entity record
   * @return update date, else creation date (UTC)
   */
  public static LocalDate lastActivity(BaseEntity entity) {
    Instant last = entity.getUpdatedAt() != null ? entity.getUpdatedAt() : entity.getCreatedAt();
    return last.atZone(ZoneOffset.UTC).toLocalDate();
  }

  private static <E extends Enum<E>> Set<E> statuses(RetentionCriteria criteria, Class<E> type) {
    Set<String> known =
        Arrays.stream(type.getEnumConstants()).map(Enum::name).collect(Collectors.toSet());
    return criteria.statuses().stream()
        .filter(known::contains)
        .map(s -> Enum.valueOf(type, s))
        .collect(Collectors.toSet());
  }

  private static <T, E> Specification<T> eligible(Set<E> statuses, LocalDate cutoff) {
    Instant limit = cutoff.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    return (root, query, cb) ->
        cb.and(
            root.get(STATUS).in(statuses),
            cb.lessThan(
                cb.coalesce(root.<Instant>get(UPDATED_AT), root.<Instant>get(CREATED_AT)), limit));
  }
}
