package com.iortatechnxt.brokerverse.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Several {@link OwnedRecord} declarations on one controller or endpoint. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface OwnedRecords {

  /**
   * The declarations.
   *
   * @return declarations
   */
  OwnedRecord[] value();
}
