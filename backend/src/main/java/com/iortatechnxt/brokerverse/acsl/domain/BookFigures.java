package com.iortatechnxt.brokerverse.acsl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * BDOI's figures of an invoice in an SOA reconciliation (ACSL 2.14.1, Appendix C): booked premium,
 * outstanding premium, amount due to the insurer not yet remitted, amount remitted with its batch
 * and date, the 2307 amount with its batch and date, the cancellation reference and the direct
 * billed indicator.
 *
 * @param premium booked gross premium
 * @param outstanding premium not yet collected
 * @param forRemittance due to the insurer, not yet remitted
 * @param remitted remitted to the insurer
 * @param remittanceBatchNo latest remittance batch
 * @param remittanceDate latest remittance date
 * @param pr2307Amount creditable withholding (2307) of the premium
 * @param pr2307BatchNo 2307 batch
 * @param pr2307Date 2307 date
 * @param cancellationRef cancellation invoice of the family
 * @param directBilled direct billed indicator
 */
@Embeddable
public record BookFigures(
    @Column(name = "book_premium", precision = 19, scale = 2) BigDecimal premium,
    @Column(name = "book_outstanding", precision = 19, scale = 2) BigDecimal outstanding,
    @Column(name = "for_remittance", precision = 19, scale = 2) BigDecimal forRemittance,
    @Column(precision = 19, scale = 2) BigDecimal remitted,
    @Column(name = "remittance_batch_no", length = 40) String remittanceBatchNo,
    @Column(name = "remittance_date") LocalDate remittanceDate,
    @Column(name = "pr2307_amount", precision = 19, scale = 2) BigDecimal pr2307Amount,
    @Column(name = "pr2307_batch_no", length = 40) String pr2307BatchNo,
    @Column(name = "pr2307_date") LocalDate pr2307Date,
    @Column(name = "cancellation_ref", length = 40) String cancellationRef,
    @Column(name = "direct_billed", nullable = false) boolean directBilled) {

  /** No book figures (line not found). */
  public static final BookFigures NONE =
      new BookFigures(null, null, null, null, null, null, null, null, null, null, false);
}
