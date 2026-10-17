package com.iortatechnxt.brokerverse.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method that checks the data scope itself, with an explicit {@link
 * DataScope#requireCompany} or {@link DataScope#requireBranch} call, because its company reaches it
 * in a way the central checks do not read (a body they cannot see, a parameter bound under another
 * name). The architecture rule accepts such a method; every other method that takes a company must
 * be covered by the data scope interceptor or body advice.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CompanyScoped {

  /**
   * Where the method checks the scope (for the reviewer).
   *
   * @return short description
   */
  String value();
}
