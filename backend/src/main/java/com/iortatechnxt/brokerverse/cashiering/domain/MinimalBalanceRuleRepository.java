package com.iortatechnxt.brokerverse.cashiering.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/** Minimal balance rules (CSHID.016). */
public interface MinimalBalanceRuleRepository extends JpaRepository<MinimalBalanceRule, String> {}
