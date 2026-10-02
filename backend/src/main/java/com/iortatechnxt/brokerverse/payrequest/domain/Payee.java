package com.iortatechnxt.brokerverse.payrequest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Who is paid and how (Appendix D RRF / RFP; MKT 2.25.0 CA / SA information).
 *
 * @param type client or employee
 * @param code client code or employee number (the Disbursement payee code)
 * @param name payee name
 * @param mode payment mode (LOV {@code PRQ_PAYMENT_MODE})
 * @param accountNo BDO account number for a credit to account
 * @param accountName account name, or the check name
 */
@Embeddable
public record Payee(
    @Enumerated(EnumType.STRING) @Column(name = "payee_type", nullable = false, length = 10)
        PayeeType type,
    @Column(name = "payee_code", nullable = false, length = 30) String code,
    @Column(name = "payee_name", nullable = false, length = 250) String name,
    @Column(name = "payment_mode", nullable = false, length = 20) String mode,
    @Column(name = "account_no", length = 40) String accountNo,
    @Column(name = "account_name", length = 250) String accountName) {

  /** Credit to a current or savings account. */
  public static final String CTA = "CTA";

  /** Check. */
  public static final String CHECK = "CHECK";
}
