package com.iortatechnxt.brokerverse.cashiering.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/** Disposition type rules (CSHID.024). */
public interface DispositionTypeRuleRepository extends JpaRepository<DispositionTypeRule, String> {}
