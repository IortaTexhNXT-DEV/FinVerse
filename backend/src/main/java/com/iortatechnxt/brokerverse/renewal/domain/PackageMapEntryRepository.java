package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** The PACKAGE code map. */
public interface PackageMapEntryRepository extends JpaRepository<PackageMapEntry, Long> {

  /**
   * Entries of a legacy package.
   *
   * @param companyId company
   * @param legacyPackageCode legacy package
   * @return entries
   */
  List<PackageMapEntry> findByCompanyIdAndLegacyPackageCode(
      Long companyId, String legacyPackageCode);

  /**
   * Entries of a company.
   *
   * @param companyId company
   * @return entries
   */
  List<PackageMapEntry> findByCompanyIdOrderByLegacyPackageCodeAscIdAsc(Long companyId);

  /**
   * Entries waiting for a checker.
   *
   * @param status status
   * @return entries
   */
  List<PackageMapEntry> findByRecordStatus(RecordStatus status);
}
