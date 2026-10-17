package com.iortatechnxt.brokerverse.storage.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Record classes of stored files. */
public interface RecordClassRepository extends JpaRepository<RecordClass, Long> {

  /**
   * A class by code.
   *
   * @param code code
   * @return class
   */
  Optional<RecordClass> findByCode(String code);

  /**
   * All classes by code.
   *
   * @return classes
   */
  List<RecordClass> findAllByOrderByCodeAsc();

  /**
   * The longest retention in years (online plus archive) of the active retention rules of a record
   * type ({@code nba_retention_rule}).
   *
   * @param recordType record type
   * @return years, null when the type has no active rule
   */
  @Query(
      value =
          "select max(years_online + years_archive) from nba_retention_rule"
              + " where record_type = :recordType and active",
      nativeQuery = true)
  Integer retentionYears(@Param("recordType") String recordType);
}
