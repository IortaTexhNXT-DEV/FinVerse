package com.iortatechnxt.brokerverse.cashiering.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/** Profiles of the payment file types. */
public interface ChannelProfileRepository extends JpaRepository<ChannelProfile, String> {}
