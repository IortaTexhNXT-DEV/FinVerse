package com.iortatechnxt.brokerverse.collections.files.domain;

import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile.Frequency;
import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile.Status;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Published Collections files (BRCLXN.024-029, 045). */
public interface ScheduledFileRepository extends JpaRepository<ScheduledFile, Long> {

  /**
   * The file of a report, period and scope.
   *
   * @param companyId company
   * @param reportCode report
   * @param periodKey period
   * @param scope scope
   * @return file
   */
  Optional<ScheduledFile> findByCompanyIdAndReportCodeAndPeriodKeyAndScope(
      Long companyId, String reportCode, String periodKey, String scope);

  /**
   * Files of a company, newest first, optionally of some frequencies.
   *
   * @param companyId company
   * @param frequencies frequencies
   * @param pageable page
   * @return files
   */
  Page<ScheduledFile> findByCompanyIdAndFrequencyInOrderByIdDesc(
      Long companyId, Collection<Frequency> frequencies, Pageable pageable);

  /**
   * Counts the files published since a time that are available now (home tile).
   *
   * @param companyId company
   * @param status PUBLISHED
   * @param since created since
   * @param now current time
   * @return count
   */
  @Query(
      "select count(f) from ScheduledFile f where f.companyId = :companyId and f.status = :status"
          + " and f.createdAt >= :since and (f.availableFrom is null or f.availableFrom <= :now)")
  long countReady(Long companyId, Status status, Instant since, Instant now);
}
