package com.iortatechnxt.brokerverse.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Provisioning events received. */
public interface IdentityEventRepository
    extends JpaRepository<IdentityEvent, Long>, JpaSpecificationExecutor<IdentityEvent> {}
