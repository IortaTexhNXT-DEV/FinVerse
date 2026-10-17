package com.iortatechnxt.brokerverse.screening.config.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Rows of {@link StrLayoutColumnRow}. */
public interface StrLayoutColumnRowRepository extends JpaRepository<StrLayoutColumnRow, Long> {

  /**
   * The rows of one owner, in display order.
   *
   * @param layoutId owner id
   * @return rows
   */
  List<StrLayoutColumnRow> findByLayoutIdOrderBySortOrderAscIdAsc(Long layoutId);

  /**
   * The rows of several owners.
   *
   * @param ids owner ids
   * @return rows
   */
  List<StrLayoutColumnRow> findByLayoutIdIn(Collection<Long> ids);

  /**
   * Deletes the rows of one owner at once (a draft's rows are replaced as a whole).
   *
   * @param layoutId owner id
   * @return rows deleted
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from StrLayoutColumnRow r where r.layoutId = :layoutId")
  int deleteByLayoutId(@Param("layoutId") Long layoutId);
}
