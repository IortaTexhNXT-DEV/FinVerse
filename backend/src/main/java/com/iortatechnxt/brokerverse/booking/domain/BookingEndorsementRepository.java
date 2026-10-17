package com.iortatechnxt.brokerverse.booking.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Endorsements and cancellations. */
public interface BookingEndorsementRepository extends JpaRepository<BookingEndorsement, Long> {

  /**
   * Endorsements of an account, oldest first.
   *
   * @param arn Account Reference Number
   * @return endorsements
   */
  List<BookingEndorsement> findByArnOrderByIdAsc(String arn);

  /**
   * Endorsements of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return page
   */
  Page<BookingEndorsement> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);

  /**
   * The endorsement posted for a caller's idempotency key.
   *
   * @param companyId company
   * @param sourceReference key
   * @return endorsement
   */
  Optional<BookingEndorsement> findByCompanyIdAndSourceReference(
      Long companyId, String sourceReference);
}
