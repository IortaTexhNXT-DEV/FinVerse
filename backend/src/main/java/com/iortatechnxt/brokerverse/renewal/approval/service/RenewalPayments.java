package com.iortatechnxt.brokerverse.renewal.approval.service;

import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The payment status of a renewal account (FRRN.028.01), from the payments applied in Cashiering to
 * its renewal account: Paid when nothing is outstanding, Partially Paid when part of the total
 * premium is outstanding, Unpaid when the whole premium is outstanding.
 */
@Component
public class RenewalPayments {

  /** Status: nothing outstanding. */
  public static final String PAID = "PAID";

  /** Status: part of the premium outstanding. */
  public static final String PARTIALLY_PAID = "PARTIALLY_PAID";

  /** Status: the whole premium outstanding. */
  public static final String UNPAID = "UNPAID";

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the reader.
   *
   * @param jdbc account premium and payments
   */
  public RenewalPayments(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The payment status of a renewal account.
   *
   * @param c renewal
   * @return status with the amounts
   */
  public Status of(RenewalCandidate c) {
    if (c.getRenewalArn() == null) {
      BigDecimal premium =
          c.getSnapshot().premium() == null ? null : c.getSnapshot().premium().grossPremium();
      return new Status(UNPAID, premium, BigDecimal.ZERO, premium);
    }
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "select a.gross_premium as premium, (select coalesce(sum(e.amount), 0)"
                + " from plc_payment_evidence e where e.arn = a.arn and e.kind = 'PAYMENT') as paid"
                + " from acc_account a where a.arn = :arn and a.company_id = :companyId",
            Map.of("arn", c.getRenewalArn(), "companyId", c.getCompanyId()));
    if (rows.isEmpty()) {
      return new Status(UNPAID, null, BigDecimal.ZERO, null);
    }
    BigDecimal premium = (BigDecimal) rows.get(0).get("premium");
    BigDecimal paid = (BigDecimal) rows.get(0).get("paid");
    return status(premium, paid);
  }

  /**
   * The status of a premium and the amount paid.
   *
   * @param premium total premium
   * @param paid amount paid
   * @return status
   */
  public static Status status(BigDecimal premium, BigDecimal paid) {
    BigDecimal total = premium == null ? BigDecimal.ZERO : premium;
    BigDecimal applied = paid == null ? BigDecimal.ZERO : paid;
    BigDecimal outstanding = total.subtract(applied).max(BigDecimal.ZERO);
    String status;
    if (outstanding.signum() == 0 && total.signum() > 0) {
      status = PAID;
    } else if (outstanding.compareTo(total) < 0) {
      status = PARTIALLY_PAID;
    } else {
      status = UNPAID;
    }
    return new Status(status, total, applied, outstanding);
  }

  /**
   * A payment status.
   *
   * @param status PAID, PARTIALLY_PAID or UNPAID
   * @param premium total premium
   * @param paid amount paid
   * @param outstanding outstanding balance
   */
  public record Status(
      String status, BigDecimal premium, BigDecimal paid, BigDecimal outstanding) {}
}
