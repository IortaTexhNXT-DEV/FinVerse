package com.iortatechnxt.brokerverse.reserves.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Surplus (Mudharabah) of one expiring takaful policy, base currency (PGIBR074).
 *
 * <p>BrokerVerse rule: applicable contribution = gross − discount + loading − commission − claims;
 * surplus = applicable − retakaful; payable before tax = surplus × participants' share % when the
 * surplus is positive, else zero; tax = payable before tax × tax %; payable = payable before tax −
 * tax.
 *
 * @param policyId policy id
 * @param policyNo policy number
 * @param insuredName insured
 * @param key reporting unit
 * @param expiryDate policy expiry
 * @param gross gross contribution (company share)
 * @param discount discount
 * @param loading loading
 * @param commission commission
 * @param claims incurred claims (paid + outstanding, net of recoveries)
 * @param applicable applicable contribution
 * @param retakaful contribution ceded to retakaful
 * @param tax tax withheld
 * @param payable surplus payable to the participant
 */
public record TakafulItem(
    Long policyId,
    String policyNo,
    String insuredName,
    ReserveKey key,
    LocalDate expiryDate,
    BigDecimal gross,
    BigDecimal discount,
    BigDecimal loading,
    BigDecimal commission,
    BigDecimal claims,
    BigDecimal applicable,
    BigDecimal retakaful,
    BigDecimal tax,
    BigDecimal payable) {}
