package com.iortatechnxt.brokerverse.bulk.service;

import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Choice;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Component;

/** The lists of values of the platform (maintained under Lists of Values) as template lists. */
@Component
class LovTemplateLists implements BulkTemplateLists {

  private final LovService lov;
  private final Clock clock;

  LovTemplateLists(LovService lov, Clock clock) {
    this.lov = lov;
    this.clock = clock;
  }

  @Override
  public List<Choice> choices(String typeCode) {
    try {
      return lov.options(typeCode, BusinessClock.today(clock)).stream()
          .map(e -> new Choice(e.code(), e.label()))
          .toList();
    } catch (ResourceNotFoundException e) {
      return List.of();
    }
  }
}
