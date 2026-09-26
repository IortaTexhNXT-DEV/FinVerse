package com.iortatechnxt.brokerverse.brokerclaims.report;

import java.time.Instant;
import java.util.List;

/**
 * A contributor to the Claims Activity Log (BRCLM.041/042, NFR 15.08; FR-CM-066): the report lists
 * the status changes, field changes and diary entries of the claims itself and adds the activities
 * of every bean implementing this port. Wave CL1-A contributes the insurer updates ({@code
 * bcl_insurer_update}) and the insurer location reference changes ({@code bcl_location_ref}) with
 * their old and new values.
 */
public interface ClaimActivitySource {

  /**
   * The activities of a period.
   *
   * @param query company, period and optional user and claim number
   * @return activities (any order; the report sorts them by time)
   */
  List<ClaimActivity> activities(ActivityQuery query);

  /**
   * What the report asks for.
   *
   * @param companyId company
   * @param from first instant (inclusive)
   * @param to last instant (exclusive)
   * @param user user who made the change, null for any
   * @param claimNo claim number, null for any
   */
  record ActivityQuery(Long companyId, Instant from, Instant to, String user, String claimNo) {}

  /**
   * One activity.
   *
   * @param at time
   * @param user user
   * @param claimNo claim number (or the ARN for a location reference of the account)
   * @param activity kind, e.g. "Insurer update" or "Location reference"
   * @param detail what changed (old and new value)
   * @param remark remark or reason
   */
  record ClaimActivity(
      Instant at, String user, String claimNo, String activity, String detail, String remark) {}
}
