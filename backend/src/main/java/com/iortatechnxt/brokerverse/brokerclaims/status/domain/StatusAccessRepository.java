package com.iortatechnxt.brokerverse.brokerclaims.status.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** The status access matrix ({@code bcl_status_access}, BRCLM.012/013). */
public interface StatusAccessRepository extends JpaRepository<StatusAccess, Long> {

  /**
   * Rows of a set of roles (every record status; the caller keeps the active ones).
   *
   * @param roleCodes roles of the user
   * @return rows
   */
  List<StatusAccess> findByRoleCodeIn(Collection<String> roleCodes);

  /**
   * The whole matrix in status, role and unit order.
   *
   * @return rows
   */
  List<StatusAccess> findAllByOrderByStatusCodeAscRoleCodeAscUnitCodeAsc();

  /**
   * The rows of a status and role (one per unit, and the any-unit row).
   *
   * @param statusCode status
   * @param roleCode role
   * @return rows
   */
  List<StatusAccess> findByStatusCodeAndRoleCode(String statusCode, String roleCode);
}
