package com.iortatechnxt.brokerverse.workflow;

import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Collects transition events of test records (listener contract for business modules). */
@Component
public class TransitionCapture {

  private final List<WorkCaseTransitioned> events = new CopyOnWriteArrayList<>();

  List<WorkCaseTransitioned> events() {
    return events;
  }

  @EventListener
  void on(WorkCaseTransitioned event) {
    if ("WorkflowTestRecord".equals(event.entityType())) {
      events.add(event);
    }
  }
}
