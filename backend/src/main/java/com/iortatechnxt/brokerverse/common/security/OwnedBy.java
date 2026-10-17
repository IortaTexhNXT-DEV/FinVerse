package com.iortatechnxt.brokerverse.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a stored record that carries no company itself and belongs to the company of a parent
 * record (a line of a batch, a contact of an insurer). The record scope check reads the parent id
 * from the named accessor and resolves the owner of the parent.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OwnedBy {

  /**
   * The parent record (entity).
   *
   * @return parent entity class
   */
  Class<?> value();

  /**
   * The property of this record holding the parent id (read with its getter).
   *
   * @return property name, e.g. {@code insurerId}
   */
  String key();
}
