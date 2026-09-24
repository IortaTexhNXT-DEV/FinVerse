package com.iortatechnxt.brokerverse.opsledger;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInContext;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/**
 * Test feed handler: one record per line "key;value"; the value FAIL fails the record, the value
 * BROKEN breaks the run.
 */
@Component
public class TestFlowInHandler implements FlowInHandler {

  /** Test feed (inserted by the tests). */
  public static final String FEED = "TEST_FLOW_IN";

  private final List<String> processed = new CopyOnWriteArrayList<>();

  @Override
  public String feedCode() {
    return FEED;
  }

  @Override
  public void handle(FlowInFile file, FlowInContext context) {
    for (String line : new String(file.content(), StandardCharsets.UTF_8).split("\n")) {
      String[] parts = line.split(";");
      if ("BROKEN".equals(parts[1].strip())) {
        throw new IllegalStateException("Broken file");
      }
      context.accept(
          parts[0],
          line,
          () -> {
            if ("FAIL".equals(parts[1].strip())) {
              throw new BusinessRuleException("TEST_RECORD", "Record " + parts[0] + " refused");
            }
            processed.add(parts[0]);
            return "REF-" + parts[0];
          });
    }
  }

  /** Keys processed. */
  public List<String> processed() {
    return processed;
  }
}
