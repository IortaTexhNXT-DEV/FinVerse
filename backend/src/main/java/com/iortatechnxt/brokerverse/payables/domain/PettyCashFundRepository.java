package com.iortatechnxt.brokerverse.payables.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link PettyCashFund}. */
public interface PettyCashFundRepository extends JpaRepository<PettyCashFund, Long> {

  /**
   * Lists the funds of a company.
   *
   * @param companyId company
   * @return funds by code
   */
  List<PettyCashFund> findByCompanyIdOrderByCode(Long companyId);

  /**
   * Finds a fund by code.
   *
   * @param companyId company
   * @param code code
   * @return fund
   */
  Optional<PettyCashFund> findByCompanyIdAndCode(Long companyId, String code);

  /**
   * Loads a fund for update so concurrent vouchers cannot overdraw the box.
   *
   * @param id fund
   * @return fund
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select f from PettyCashFund f where f.id = :id")
  Optional<PettyCashFund> lockById(@Param("id") Long id);
}
