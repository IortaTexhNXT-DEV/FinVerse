package com.iortatechnxt.brokerverse.nbreport.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Saved report variants. */
public interface ReportVariantRepository extends JpaRepository<ReportVariant, Long> {

  /**
   * The variants of a report a user sees: their own and the shared ones.
   *
   * @param reportCode report
   * @param owner user
   * @return variants by name
   */
  @Query(
      "select v from ReportVariant v where v.reportCode = :reportCode"
          + " and (lower(v.owner) = lower(:owner) or v.shared = true)"
          + " order by v.standard desc, case when v.standard = true then v.id else 0 end,"
          + " lower(v.name)")
  List<ReportVariant> visibleTo(String reportCode, String owner);

  /**
   * A user's variant by name.
   *
   * @param owner owner
   * @param reportCode report
   * @param name name
   * @return variant
   */
  Optional<ReportVariant> findByOwnerIgnoreCaseAndReportCodeAndNameIgnoreCase(
      String owner, String reportCode, String name);

  /**
   * A user's variants of a report.
   *
   * @param owner owner
   * @param reportCode report
   * @return variants
   */
  List<ReportVariant> findByOwnerIgnoreCaseAndReportCode(String owner, String reportCode);
}
