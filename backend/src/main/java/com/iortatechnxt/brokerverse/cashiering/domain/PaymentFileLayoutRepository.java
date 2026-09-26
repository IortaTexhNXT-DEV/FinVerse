package com.iortatechnxt.brokerverse.cashiering.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/** Payment file layouts (CSHID.008). */
public interface PaymentFileLayoutRepository extends JpaRepository<PaymentFileLayout, String> {}
