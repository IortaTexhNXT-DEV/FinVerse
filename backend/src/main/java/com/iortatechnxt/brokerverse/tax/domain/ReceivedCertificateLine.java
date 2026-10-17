package com.iortatechnxt.brokerverse.tax.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;

/**
 * An income payment of a received BIR 2307 certificate (DIS 2.11.1): the kind of income (commission
 * or incentive), the ATC, the income and the tax withheld.
 *
 * @param lineNo line number
 * @param kind commission or incentive
 * @param atc alphanumeric tax code
 * @param incomeNature nature of the income payment
 * @param income income payment
 * @param tax tax withheld
 */
@Embeddable
public record ReceivedCertificateLine(
    @Column(name = "line_no", nullable = false) int lineNo,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) Kind kind,
    @Column(nullable = false, length = 10) String atc,
    @Column(name = "income_nature", length = 200) String incomeNature,
    @Column(nullable = false, precision = 19, scale = 2) BigDecimal income,
    @Column(nullable = false, precision = 19, scale = 2) BigDecimal tax) {

  /** The income a certificate covers. */
  public enum Kind {
    /** Commission earned from the insurer: AR-BIR on commission. */
    COMMISSION,
    /** Incentives earned from the insurer: AR-BIR on incentives. */
    INCENTIVE
  }
}
