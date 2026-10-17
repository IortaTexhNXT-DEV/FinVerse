package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.audit.service.AuditSubjects;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The client or assured's name of the Product Maintenance records in the audit logs (BDOI FRS
 * FRPM.021.01): the client of a package request (the programme title when generic), the assured of
 * a quotation request, the package of a deactivation request and the name of a product.
 */
@Component
public class PmAuditSubjects implements AuditSubjects {

  private static final String PACKAGE_SQL =
      "select request_no as k, coalesce(client_name, title) as v from pm_request"
          + " where request_no in (:ids)";

  private static final String CASE_SQL =
      "select r.request_no as k, coalesce(r.client_name, r.title) as v from pm_request r"
          + " where r.request_no in (:ids) union all select p.prf_no, p.client_name"
          + " from npk_proposal p where p.prf_no in (:ids)";

  private static final String QUOTATION_SQL =
      "select prf_no as k, client_name as v from npk_proposal where prf_no in (:ids)"
          + " union all select cast(id as varchar), client_name from npk_proposal"
          + " where cast(id as varchar) in (:ids)";

  private static final String DEACTIVATION_SQL =
      "select request_no as k, package_name as v from pm_deactivation_request"
          + " where request_no in (:ids)";

  private static final String PRODUCT_SQL =
      "select code as k, name as v from cat_product where code in (:ids)";

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the lookup.
   *
   * @param jdbc JDBC template
   */
  public PmAuditSubjects(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Map<String, String> subjects(String entityType, Collection<String> entityIds) {
    Map<String, String> found = new HashMap<>();
    if (entityIds.isEmpty()) {
      return found;
    }
    MapSqlParameterSource ids = new MapSqlParameterSource("ids", entityIds);
    RowCallbackHandler keep = rs -> found.putIfAbsent(rs.getString("k"), rs.getString("v"));
    switch (entityType) {
      case PackageRequests.ENTITY -> jdbc.query(PACKAGE_SQL, ids, keep);
      case PmAuditScope.WORK_CASE -> jdbc.query(CASE_SQL, ids, keep);
      case "ProposalRequest" -> jdbc.query(QUOTATION_SQL, ids, keep);
      case DeactivationService.ENTITY -> jdbc.query(DEACTIVATION_SQL, ids, keep);
      case "Product" -> jdbc.query(PRODUCT_SQL, ids, keep);
      default -> {
        // not a Product Maintenance record
      }
    }
    return found;
  }
}
