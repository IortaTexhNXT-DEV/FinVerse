package com.iortatechnxt.brokerverse.brokerclaims.claim.service;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Handling unit and branch of a claims user (NFR p.37; BRCLM.012): the unit from the claims handler
 * register {@code bcl_handler} (V1020, maintained on Claims Setup) and the user's home branch. A
 * user outside the register handles claims without a unit.
 */
@Component
public class HandlerDirectory {

  private static final String SQL =
      "select h.unit_code, u.home_branch_id from sec_user u"
          + " left join bcl_handler h on h.username = u.username and h.active"
          + " where u.username = ?";

  private final JdbcTemplate jdbc;

  /**
   * Creates the directory.
   *
   * @param jdbc register reads
   */
  public HandlerDirectory(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Unit and branch of a user.
   *
   * @param username user
   * @return unit and branch, both null for an unknown user
   */
  public Handler of(String username) {
    List<Handler> found =
        jdbc.query(
            SQL,
            (rs, n) -> new Handler(rs.getString(1), rs.getObject(2, Long.class)),
            username);
    return found.isEmpty() ? new Handler(null, null) : found.get(0);
  }

  /**
   * Where a handler works.
   *
   * @param unitCode claims unit ({@code BCL_UNIT}), null outside the register
   * @param branchId home branch, may be null
   */
  public record Handler(String unitCode, Long branchId) {}
}
