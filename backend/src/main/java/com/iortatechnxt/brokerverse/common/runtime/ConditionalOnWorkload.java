package com.iortatechnxt.brokerverse.common.runtime;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Conditional;

/**
 * Registers a bean or configuration only when the instance's {@link RuntimeRole} runs at least one
 * of the given workloads. This is how the role switches Kafka consumers and scheduling on or off:
 * business code never tests the role itself.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnWorkloadCondition.class)
public @interface ConditionalOnWorkload {

  /**
   * Workloads, any of which enables the bean.
   *
   * @return workloads
   */
  Workload[] value();
}
