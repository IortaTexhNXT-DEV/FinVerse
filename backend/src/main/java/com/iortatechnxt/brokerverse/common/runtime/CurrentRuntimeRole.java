package com.iortatechnxt.brokerverse.common.runtime;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** The {@link RuntimeRole} of this instance, resolved once at start-up. */
@Component
public class CurrentRuntimeRole {

  private final RuntimeRole role;

  /**
   * Resolves the role.
   *
   * @param environment environment ({@value RuntimeRole#PROPERTY})
   */
  public CurrentRuntimeRole(Environment environment) {
    this.role = RuntimeRole.of(environment);
  }

  /**
   * The role.
   *
   * @return role
   */
  public RuntimeRole role() {
    return role;
  }

  /**
   * Tells whether this instance runs a workload.
   *
   * @param workload workload
   * @return true when it does
   */
  public boolean runs(Workload workload) {
    return role.runs(workload);
  }
}
