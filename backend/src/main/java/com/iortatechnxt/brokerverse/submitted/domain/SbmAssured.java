package com.iortatechnxt.brokerverse.submitted.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The assured of a submitted policy and how to reach them (BRIDSP-04).
 *
 * @param assuredName assured (client name)
 * @param mailingAddress mailing address
 * @param telephone telephone
 * @param mobile mobile
 * @param email e-mail
 * @param bankCounterpartEmail e-mail of the bank counterpart (findings and IAAF)
 */
@Embeddable
public record SbmAssured(
    @Column(name = "assured_name", nullable = false, length = 250) String assuredName,
    @Column(name = "mailing_address", length = 500) String mailingAddress,
    @Column(name = "telephone", length = 40) String telephone,
    @Column(name = "mobile", length = 40) String mobile,
    @Column(name = "email", length = 150) String email,
    @Column(name = "bank_counterpart_email", length = 150) String bankCounterpartEmail) {}
