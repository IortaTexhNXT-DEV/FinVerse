package com.iortatechnxt.brokerverse.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Names the record a path variable of an endpoint addresses by its id, so that the record scope
 * check can refuse a record of a company outside the data scope of the signed-in user before the
 * controller runs (DATA_SCOPE_DESIGN.md section 4.1). The refusal is HTTP 404, as if the record did
 * not exist. On a controller class the declaration applies to every endpoint with that path
 * variable; a declaration on the method replaces the class declaration of the same variable.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(OwnedRecords.class)
public @interface OwnedRecord {

  /**
   * The stored record (entity) the id belongs to.
   *
   * @return entity class, {@code void} when the record is named by {@link #type()}
   */
  Class<?> value() default void.class;

  /**
   * The record type of a record kept outside the entity model, resolved by the {@link
   * RecordOwnerLookup} of that type.
   *
   * @return record type, empty when the record is named by {@link #value()}
   */
  String type() default "";

  /**
   * The path variable holding the id.
   *
   * @return path variable name
   */
  String variable() default "id";
}
