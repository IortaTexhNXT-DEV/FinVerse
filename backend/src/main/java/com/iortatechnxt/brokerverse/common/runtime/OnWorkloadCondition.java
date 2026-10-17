package com.iortatechnxt.brokerverse.common.runtime;

import java.util.Arrays;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Condition of {@link ConditionalOnWorkload}: matches when the role runs any listed workload. */
class OnWorkloadCondition extends SpringBootCondition {

  @Override
  public ConditionOutcome getMatchOutcome(
      ConditionContext context, AnnotatedTypeMetadata metadata) {
    Map<String, Object> attributes =
        metadata.getAnnotationAttributes(ConditionalOnWorkload.class.getName());
    Workload[] workloads =
        attributes == null ? new Workload[0] : (Workload[]) attributes.get("value");
    RuntimeRole role = RuntimeRole.of(context.getEnvironment());
    boolean match = Arrays.stream(workloads).anyMatch(role::runs);
    String message =
        "runtime role " + role + (match ? " runs " : " runs none of ") + Arrays.toString(workloads);
    return match ? ConditionOutcome.match(message) : ConditionOutcome.noMatch(message);
  }
}
