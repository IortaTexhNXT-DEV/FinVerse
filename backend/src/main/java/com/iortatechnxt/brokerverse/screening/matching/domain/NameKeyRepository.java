package com.iortatechnxt.brokerverse.screening.matching.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Blocking keys of the matching engine (SNSRP-301). */
public interface NameKeyRepository extends JpaRepository<NameKey, Long> {

  /**
   * Removes the keys of subjects.
   *
   * @param kinds subject kinds
   * @param ids subject ids
   * @return rows removed
   */
  @Modifying
  @Query("delete from NameKey k where k.subjectKind in :kinds and k.subjectId in :ids")
  int deleteBySubjects(
      @Param("kinds") Collection<NameSubjectKind> kinds, @Param("ids") Collection<Long> ids);

  /**
   * The subjects of some kinds that have one of the key values of a type.
   *
   * @param kinds subject kinds
   * @param type key type
   * @param values key values
   * @return distinct subject ids
   */
  @Query(
      "select distinct k.subjectId from NameKey k where k.subjectKind in :kinds"
          + " and k.keyType = :type and k.keyValue in :values")
  List<Long> subjectsWith(
      @Param("kinds") Collection<NameSubjectKind> kinds,
      @Param("type") NameKeyType type,
      @Param("values") Collection<String> values);

  /**
   * The subjects of a kind that have keys, among some ids.
   *
   * @param kind subject kind
   * @param ids subject ids
   * @return distinct subject ids that have keys
   */
  @Query(
      "select distinct k.subjectId from NameKey k where k.subjectKind = :kind"
          + " and k.subjectId in :ids")
  List<Long> keyed(@Param("kind") NameSubjectKind kind, @Param("ids") Collection<Long> ids);

  /**
   * The subjects whose keys were built after a time (entries changed since the last run).
   *
   * @param kind subject kind
   * @param since the time
   * @return distinct subject ids
   */
  @Query(
      "select distinct k.subjectId from NameKey k where k.subjectKind = :kind"
          + " and k.createdAt > :since")
  List<Long> keyedSince(@Param("kind") NameSubjectKind kind, @Param("since") Instant since);

  /**
   * The ACTIVE watchlist entries without keys (loaded outside the list maintenance, for example by
   * a migration).
   *
   * @return entry ids
   */
  @Query(
      value =
          "select e.id from scr_watchlist_entry e where e.status = 'ACTIVE' and not exists"
              + " (select 1 from scr_name_key k where k.subject_kind = 'ENTRY'"
              + " and k.subject_id = e.id) order by e.id",
      nativeQuery = true)
  List<Long> activeEntriesWithoutKeys();
}
