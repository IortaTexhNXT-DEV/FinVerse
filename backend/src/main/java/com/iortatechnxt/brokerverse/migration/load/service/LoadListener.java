package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;

/**
 * A step run after a batch is loaded (the reconciliation), in the transaction of the end of the
 * load.
 */
public interface LoadListener {

  /**
   * The batch was loaded.
   *
   * @param batch batch
   */
  void loaded(MigBatch batch);
}
