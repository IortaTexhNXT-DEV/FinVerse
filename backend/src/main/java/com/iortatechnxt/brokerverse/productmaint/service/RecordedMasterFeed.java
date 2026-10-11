package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.productmaint.domain.MasterChange;
import com.iortatechnxt.brokerverse.productmaint.domain.MasterChangeRepository;
import org.springframework.stereotype.Component;

/**
 * The product master feed of BDOI's FRS (FRPM.029.01): every release, retirement or expiry of a
 * package is recorded for the next transfer to the other BDOI systems.
 */
@Component
public class RecordedMasterFeed implements ProductMasterFeed {

  private final MasterChangeRepository changes;

  /**
   * Creates the feed.
   *
   * @param changes product master changes
   */
  public RecordedMasterFeed(MasterChangeRepository changes) {
    this.changes = changes;
  }

  @Override
  public void publish(ProductMasterChange change) {
    changes.save(
        new MasterChange(
            change.productCode(),
            change.versionNo(),
            change.kind(),
            change.effectiveDate(),
            change.sourceRequestNo()));
  }
}
