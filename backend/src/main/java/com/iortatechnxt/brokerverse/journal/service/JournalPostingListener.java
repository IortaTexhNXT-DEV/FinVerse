package com.iortatechnxt.brokerverse.journal.service;

import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;

/**
 * Port notified after a journal has been posted, inside the posting transaction (implemented by the
 * alert engine for posting-time exception rules). Implementations must be quick and must not throw
 * for ordinary conditions: an exception rolls the posting back.
 */
public interface JournalPostingListener {

  /**
   * Handles a posted journal.
   *
   * @param batch posted batch (lines loaded)
   */
  void onPosted(JournalBatch batch);
}
