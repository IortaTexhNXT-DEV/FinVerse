package com.iortatechnxt.brokerverse.collections.worklist.service;

import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItemRepository;
import com.iortatechnxt.brokerverse.collections.common.service.ClxSettings;
import com.iortatechnxt.brokerverse.collections.common.service.CollectionItems;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The soft record lock of a collection account (BRCLXN NFR UI/UX: "&lt;Username&gt; is editing"):
 * opening the account for work takes the lock for {@code CLX_EDIT_LOCK_MINUTES}; while it is held
 * other users see who is editing and their changes are refused. The lock is refreshed while the
 * holder works and released when they leave; an expired lock may be taken by anyone.
 */
@Service
@Transactional
public class EditLockService {

  private final CollectionItemRepository items;
  private final ClxSettings settings;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param items items
   * @param settings parameters
   * @param currentUser signed-in user
   * @param clock clock
   */
  public EditLockService(
      CollectionItemRepository items, ClxSettings settings, CurrentUser currentUser, Clock clock) {
    this.items = items;
    this.settings = settings;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Takes or refreshes the lock, unless another user holds it.
   *
   * @param invoiceNo invoice
   * @return who holds the lock now
   */
  public LockState acquire(String invoiceNo) {
    CollectionItem item = lock(invoiceNo);
    String me = currentUser.username();
    Instant now = clock.instant();
    if (!item.lockedByOther(me, now, settings.editLock())) {
      item.lock(me, now);
    }
    return state(item, me);
  }

  /**
   * Releases the user's lock.
   *
   * @param invoiceNo invoice
   * @return who holds the lock now
   */
  public LockState release(String invoiceNo) {
    CollectionItem item = lock(invoiceNo);
    String me = currentUser.username();
    item.unlock(me);
    return state(item, me);
  }

  /**
   * Who holds the lock of an item, as seen by the signed-in user.
   *
   * @param item item
   * @return state; an expired lock is shown as free
   */
  @Transactional(readOnly = true)
  public LockState stateOf(CollectionItem item) {
    return state(item, currentUser.username());
  }

  private LockState state(CollectionItem item, String me) {
    Instant now = clock.instant();
    boolean held =
        item.getEditingBy() != null
            && item.getEditingSince() != null
            && item.getEditingSince().plus(settings.editLock()).isAfter(now);
    if (!held) {
      return new LockState(null, null, false);
    }
    return new LockState(
        item.getEditingBy(), item.getEditingSince(), CurrentUser.sameUser(item.getEditingBy(), me));
  }

  private CollectionItem lock(String invoiceNo) {
    return items
        .lockByInvoiceNo(invoiceNo)
        .orElseThrow(() -> new ResourceNotFoundException(CollectionItems.ENTITY, invoiceNo));
  }

  /**
   * The edit lock of an account.
   *
   * @param editingBy holder, null when free
   * @param since taken at
   * @param mine whether the signed-in user holds it
   */
  public record LockState(String editingBy, Instant since, boolean mine) {}
}
