package com.iortatechnxt.brokerverse.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller or an endpoint whose path ids address programme-wide records (global masters,
 * users and roles, platform administration) that belong to no company, so the record scope check
 * has nothing to refuse. This is the explicit allow list of the architecture rule that every
 * endpoint addressing a record by id either names the record ({@link OwnedRecord}) or says why it
 * needs no scope.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface GlobalRecord {

  /**
   * Why the records are programme-wide (for the reviewer).
   *
   * @return short reason
   */
  String value();
}
