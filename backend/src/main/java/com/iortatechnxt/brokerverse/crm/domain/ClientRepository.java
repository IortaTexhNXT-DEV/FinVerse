package com.iortatechnxt.brokerverse.crm.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Clients. */
public interface ClientRepository
    extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {

  /**
   * A client by client code or prospect code.
   *
   * @param companyId company
   * @param code client or prospect code
   * @return client
   */
  @Query(
      "select c from Client c where c.companyId = :companyId"
          + " and (c.clientCode = :code or c.prospectCode = :code)")
  Optional<Client> findByCode(@Param("companyId") Long companyId, @Param("code") String code);
}
