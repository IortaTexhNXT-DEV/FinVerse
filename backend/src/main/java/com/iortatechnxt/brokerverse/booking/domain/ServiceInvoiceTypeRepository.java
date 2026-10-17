package com.iortatechnxt.brokerverse.booking.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Service invoice types. */
public interface ServiceInvoiceTypeRepository extends JpaRepository<ServiceInvoiceType, Long> {

  /**
   * A type by code.
   *
   * @param code code
   * @return type
   */
  Optional<ServiceInvoiceType> findByCode(String code);

  /**
   * Active types issued by a trigger.
   *
   * @param trigger trigger
   * @return types
   */
  List<ServiceInvoiceType> findByTriggerAndActiveTrueOrderByCodeAsc(SiTrigger trigger);

  /**
   * Every type by code.
   *
   * @return types
   */
  List<ServiceInvoiceType> findAllByOrderByCodeAsc();
}
