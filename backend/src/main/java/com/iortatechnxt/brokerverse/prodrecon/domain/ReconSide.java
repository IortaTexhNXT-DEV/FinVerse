package com.iortatechnxt.brokerverse.prodrecon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The fields compared by the reconciliation (PRCID.027) as one side holds them: BDOI's booked
 * invoice or the insurer's production line. The item table stores both sides with the prefixes
 * {@code bdoi_} and {@code ins_}.
 *
 * @param policyNo policy number
 * @param referenceNo reference / invoice number
 * @param pnNo PN number(s)
 * @param periodFrom policy period start
 * @param periodTo policy period end
 * @param assuredName assured name
 * @param commission commission amount
 * @param basicPremium basic premium
 * @param grossPremium gross premium
 */
@Embeddable
public record ReconSide(
    @Column(name = "policy_no", length = 60) String policyNo,
    @Column(name = "reference_no", length = 40) String referenceNo,
    @Column(name = "pn_no", length = 500) String pnNo,
    @Column(name = "period_from") LocalDate periodFrom,
    @Column(name = "period_to") LocalDate periodTo,
    @Column(name = "assured_name", length = 250) String assuredName,
    @Column(precision = 19, scale = 2) BigDecimal commission,
    @Column(name = "basic_premium", precision = 19, scale = 2) BigDecimal basicPremium,
    @Column(name = "gross_premium", precision = 19, scale = 2) BigDecimal grossPremium) {}
