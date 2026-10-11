package com.iortatechnxt.brokerverse.migration.recon.service;

import com.iortatechnxt.brokerverse.migration.load.domain.MigBatch;
import java.util.List;

/**
 * An additional reconciliation of an object, run after the generic levels L1-L4: the GL level L5 of
 * the financial objects (migration clearing per branch and currency, legacy control accounts
 * against the opening detail).
 */
public interface ReconCheck {

  /**
   * Whether the check applies to the object of a batch.
   *
   * @param objectCode object
   * @return true when it applies
   */
  boolean appliesTo(String objectCode);

  /**
   * The lines of the check.
   *
   * @param batch batch
   * @return lines
   */
  List<ReconLineSpec> lines(MigBatch batch);
}
