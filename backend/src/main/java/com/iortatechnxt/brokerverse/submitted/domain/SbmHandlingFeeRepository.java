package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Handling-fee records. */
public interface SbmHandlingFeeRepository extends JpaRepository<SbmHandlingFee, Long> {

  /**
   * Billed fees of a PN.
   *
   * @param companyId company
   * @param pnNo PN
   * @param status BILLED
   * @return fees
   */
  List<SbmHandlingFee> findByCompanyIdAndPnNoAndStatus(Long companyId, String pnNo, String status);

  /**
   * Billed fees of a location reference.
   *
   * @param companyId company
   * @param locationRef location reference
   * @param status BILLED
   * @return fees
   */
  List<SbmHandlingFee> findByCompanyIdAndLocationRefAndStatus(
      Long companyId, String locationRef, String status);

  /**
   * The fee of a request reference.
   *
   * @param feeNo fee number
   * @return fee
   */
  Optional<SbmHandlingFee> findByFeeNo(String feeNo);

  /**
   * The fee tagged with an unapplied item.
   *
   * @param unappliedRef item
   * @return fees
   */
  List<SbmHandlingFee> findByUnappliedRef(String unappliedRef);

  /**
   * Fees in some statuses.
   *
   * @param companyId company
   * @param statuses statuses
   * @param pageable page
   * @return fees, newest first
   */
  Page<SbmHandlingFee> findByCompanyIdAndStatusInOrderByIdDesc(
      Long companyId, Collection<String> statuses, Pageable pageable);

  /**
   * Fees in a status of a company (tagger).
   *
   * @param companyId company
   * @param status status
   * @return count
   */
  long countByCompanyIdAndStatus(Long companyId, String status);
}
