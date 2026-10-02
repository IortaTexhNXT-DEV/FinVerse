package com.iortatechnxt.brokerverse.screening.config.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Rows of {@link StrLayoutRow}. */
public interface StrLayoutRowRepository extends JpaRepository<StrLayoutRow, Long> {

  /**
   * The rows of one owner, in display order.
   *
   * @param versionId owner id
   * @return rows
   */
  List<StrLayoutRow> findByVersionIdOrderByIdAsc(Long versionId);

  /**
   * The rows of several owners.
   *
   * @param ids owner ids
   * @return rows
   */
  List<StrLayoutRow> findByVersionIdIn(Collection<Long> ids);

  /**
   * Deletes the rows of one owner at once (a draft's rows are replaced as a whole).
   *
   * @param versionId owner id
   * @return rows deleted
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from StrLayoutRow r where r.versionId = :versionId")
  int deleteByVersionId(@Param("versionId") Long versionId);
}
