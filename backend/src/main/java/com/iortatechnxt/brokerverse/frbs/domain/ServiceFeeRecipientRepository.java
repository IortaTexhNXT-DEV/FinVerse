package com.iortatechnxt.brokerverse.frbs.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Service-fee recipients of the sales units (FRBS 2.10.0). */
public interface ServiceFeeRecipientRepository extends JpaRepository<ServiceFeeRecipient, Long> {

  /**
   * The recipients of a company.
   *
   * @param companyId company
   * @return recipients by unit
   */
  List<ServiceFeeRecipient> findByCompanyIdOrderBySalesUnitAsc(Long companyId);

  /**
   * The recipient of a unit.
   *
   * @param companyId company
   * @param salesUnit unit
   * @return recipient
   */
  Optional<ServiceFeeRecipient> findByCompanyIdAndSalesUnit(Long companyId, String salesUnit);
}
