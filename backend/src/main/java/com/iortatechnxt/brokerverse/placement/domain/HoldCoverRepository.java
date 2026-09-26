package com.iortatechnxt.brokerverse.placement.domain;

import com.iortatechnxt.brokerverse.account.domain.HoldCoverStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Hold covers. */
public interface HoldCoverRepository extends JpaRepository<HoldCover, Long> {

  /**
   * The latest hold cover of an account.
   *
   * @param arn Account Reference Number
   * @return hold cover
   */
  Optional<HoldCover> findFirstByArnOrderByIdDesc(String arn);

  /**
   * Hold covers of an account, newest first.
   *
   * @param arn ARN
   * @return hold covers
   */
  List<HoldCover> findByArnOrderByIdDesc(String arn);

  /**
   * Open hold covers expiring on or before a date, soonest first (expiry monitor).
   *
   * @param statuses open statuses
   * @param date last expiry date
   * @return hold covers
   */
  List<HoldCover> findByStatusInAndExpiryDateLessThanEqualOrderByExpiryDateAsc(
      Collection<HoldCoverStatus> statuses, LocalDate date);

  /**
   * Open hold covers of a company expiring on or before a date (workbench tab).
   *
   * @param companyId company
   * @param statuses open statuses
   * @param date last expiry date
   * @param pageable page
   * @return hold covers
   */
  Page<HoldCover> findByCompanyIdAndStatusInAndExpiryDateLessThanEqualOrderByExpiryDateAsc(
      Long companyId, Collection<HoldCoverStatus> statuses, LocalDate date, Pageable pageable);

  /**
   * Number of open hold covers of a company expiring on or before a date.
   *
   * @param companyId company
   * @param statuses open statuses
   * @param date last expiry date
   * @return count
   */
  long countByCompanyIdAndStatusInAndExpiryDateLessThanEqual(
      Long companyId, Collection<HoldCoverStatus> statuses, LocalDate date);
}
