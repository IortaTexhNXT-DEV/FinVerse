package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Account officers of the sales teams. */
public interface SalesOfficerRepository extends JpaRepository<SalesOfficer, Long> {

  /**
   * Officers of a company.
   *
   * @param companyId company
   * @return officers
   */
  List<SalesOfficer> findByCompanyIdOrderByTeamCodeAscUsernameAsc(Long companyId);

  /**
   * The team placement of a user.
   *
   * @param companyId company
   * @param username user name
   * @return officer
   */
  Optional<SalesOfficer> findByCompanyIdAndUsername(Long companyId, String username);
}
