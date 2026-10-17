package com.iortatechnxt.brokerverse.migration.mapping.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/** Survivorship rules of client matching. */
public interface SurvivorshipRuleRepository extends JpaRepository<SurvivorshipRule, Long> {}
