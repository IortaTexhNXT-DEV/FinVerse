package com.iortatechnxt.brokerverse.collections.bulk.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.util.List;

/**
 * Default {@link WorklistUpdates} while the worklist of wave C1-A is not installed: dispositions,
 * efforts and remarks are refused with a clear message, never recorded elsewhere.
 */
public class PendingWorklistUpdates implements WorklistUpdates {

  private static final String MESSAGE =
      "Dispositions, efforts and remarks are updated through the collection worklist, which is"
          + " not available";

  @Override
  public List<String> validate(Long companyId, ItemUpdate update) {
    return update.isEmpty() ? List.of() : List.of(MESSAGE);
  }

  @Override
  public String apply(Long companyId, ItemUpdate update, String bulkRef) {
    throw new BusinessRuleException("CLX_WORKLIST_UNAVAILABLE", MESSAGE);
  }
}
