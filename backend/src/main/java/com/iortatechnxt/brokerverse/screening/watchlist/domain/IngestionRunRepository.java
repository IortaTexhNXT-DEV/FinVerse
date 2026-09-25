package com.iortatechnxt.brokerverse.screening.watchlist.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Ingestion runs (SNSRP-201). */
public interface IngestionRunRepository extends JpaRepository<IngestionRun, Long> {

  /**
   * Runs, newest first, optionally of one source.
   *
   * @param sourceId source, {@code null} for all
   * @param pageable page
   * @return runs
   */
  @Query(
      "select r from IngestionRun r where (:sourceId is null or r.sourceId = :sourceId)"
          + " order by r.startedAt desc, r.id desc")
  Page<IngestionRun> search(@Param("sourceId") Long sourceId, Pageable pageable);

  /**
   * The runs that read one of the files (a staged file is read once).
   *
   * @param attachmentIds attachment ids
   * @return runs
   */
  List<IngestionRun> findByFileAttachmentIdIn(Collection<Long> attachmentIds);
}
