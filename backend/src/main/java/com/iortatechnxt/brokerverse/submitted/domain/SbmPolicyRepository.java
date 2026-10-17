package com.iortatechnxt.brokerverse.submitted.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** The Submitted Masterlist. */
public interface SbmPolicyRepository
    extends JpaRepository<SbmPolicy, Long>, JpaSpecificationExecutor<SbmPolicy> {

  /**
   * A record by its number.
   *
   * @param sbmNo masterlist number
   * @return record
   */
  Optional<SbmPolicy> findBySbmNo(String sbmNo);

  /**
   * A record by its natural key (BRIDSP-01 R2).
   *
   * @param companyId company
   * @param segment segment
   * @param businessType NB or RB
   * @param naturalKey PN or policy number key
   * @param expiryDate expiry
   * @return record
   */
  Optional<SbmPolicy> findByCompanyIdAndSegmentAndBusinessTypeAndNaturalKeyAndTermsExpiryDate(
      Long companyId,
      String segment,
      SbmBusinessType businessType,
      String naturalKey,
      LocalDate expiryDate);

  /**
   * A migrated record by its legacy reference (idempotent migration, BRIDSP-33).
   *
   * @param companyId company
   * @param legacyRef legacy reference
   * @return record
   */
  Optional<SbmPolicy> findFirstByCompanyIdAndLegacyRef(Long companyId, String legacyRef);

  /**
   * Records of some statuses (processing scope).
   *
   * @param companyId company
   * @param statuses statuses
   * @return records
   */
  List<SbmPolicy> findByCompanyIdAndStatusInOrderByIdAsc(
      Long companyId, Collection<SbmPolicyStatus> statuses);

  /**
   * Records of an intake run.
   *
   * @param intakeRunId intake run
   * @return records
   */
  List<SbmPolicy> findByIntakeRunIdOrderByIdAsc(Long intakeRunId);

  /**
   * Records of a renewal account.
   *
   * @param arn renewal account
   * @return records
   */
  List<SbmPolicy> findByRenewalArn(String arn);

  /**
   * Other records with the same PN (sanitation: duplicates).
   *
   * @param companyId company
   * @param pnNo PN
   * @param id record to leave out
   * @param open statuses of the open records
   * @return count
   */
  @Query(
      "select count(p) from SbmPolicy p where p.companyId = :companyId and p.loan.pnNo = :pnNo"
          + " and p.id <> :id and p.status in :open")
  long countSamePn(
      @Param("companyId") Long companyId,
      @Param("pnNo") String pnNo,
      @Param("id") Long id,
      @Param("open") Collection<SbmPolicyStatus> open);

  /**
   * Other open records with the same serial number (sanitation: duplicates).
   *
   * @param companyId company
   * @param serialNo serial number
   * @param id record to leave out
   * @param open statuses of the open records
   * @return count
   */
  @Query(
      "select count(p) from SbmPolicy p where p.companyId = :companyId and p.id <> :id"
          + " and p.status in :open and p.risk.serialNo = :no")
  long countSameSerial(
      @Param("companyId") Long companyId,
      @Param("no") String serialNo,
      @Param("id") Long id,
      @Param("open") Collection<SbmPolicyStatus> open);

  /**
   * Other open records with the same motor number (sanitation: duplicates).
   *
   * @param companyId company
   * @param motorNo motor number
   * @param id record to leave out
   * @param open statuses of the open records
   * @return count
   */
  @Query(
      "select count(p) from SbmPolicy p where p.companyId = :companyId and p.id <> :id"
          + " and p.status in :open and p.risk.motorNo = :no")
  long countSameMotor(
      @Param("companyId") Long companyId,
      @Param("no") String motorNo,
      @Param("id") Long id,
      @Param("open") Collection<SbmPolicyStatus> open);

  /**
   * For Renewal records expiring up to a date (expiry scan).
   *
   * @param companyId company
   * @param status FOR_RENEWAL
   * @param until last expiry date
   * @return records, earliest expiry first
   */
  @Query(
      "select p from SbmPolicy p where p.companyId = :companyId and p.status = :status"
          + " and p.terms.expiryDate <= :until order by p.terms.expiryDate, p.id")
  List<SbmPolicy> expiringUntil(
      @Param("companyId") Long companyId,
      @Param("status") SbmPolicyStatus status,
      @Param("until") LocalDate until);

  /**
   * Company ids with masterlist records (jobs).
   *
   * @return company ids
   */
  @Query("select distinct p.companyId from SbmPolicy p")
  List<Long> companies();

  /**
   * Whether a record of a company has a natural key (seed idempotency).
   *
   * @param companyId company
   * @param naturalKey PN or policy key
   * @return true when present
   */
  boolean existsByCompanyIdAndNaturalKey(Long companyId, String naturalKey);
}
