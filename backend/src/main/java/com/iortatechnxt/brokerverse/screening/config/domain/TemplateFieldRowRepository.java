package com.iortatechnxt.brokerverse.screening.config.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Rows of {@link TemplateFieldRow}. */
public interface TemplateFieldRowRepository extends JpaRepository<TemplateFieldRow, Long> {

  /**
   * The rows of one owner, in display order.
   *
   * @param templateId owner id
   * @return rows
   */
  List<TemplateFieldRow> findByTemplateIdOrderBySortOrderAscIdAsc(Long templateId);

  /**
   * The rows of several owners.
   *
   * @param ids owner ids
   * @return rows
   */
  List<TemplateFieldRow> findByTemplateIdIn(Collection<Long> ids);

  /**
   * Deletes the rows of one owner at once (a draft's rows are replaced as a whole).
   *
   * @param templateId owner id
   * @return rows deleted
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from TemplateFieldRow r where r.templateId = :templateId")
  int deleteByTemplateId(@Param("templateId") Long templateId);
}
