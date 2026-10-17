package com.iortatechnxt.brokerverse.identity.service;

import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEvent;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventRepository;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventSource;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventStatus;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventType;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/** Recognises an identity event that the sender sends again (idempotent provisioning). */
final class IdentityEventReplay {

  private IdentityEventReplay() {}

  /**
   * The earlier event an event repeats, when the sender sends it again (a retry after a time-out):
   * same source, UIDM request number, type and content as the latest event of the account of the
   * same kind (membership, or any other), and applied or with nothing to change. The answer is then
   * that event's, and nothing is recorded or changed twice. An event that differs from the latest
   * one, or follows a refusal or a failure, is processed again; an on-demand synchronisation is
   * always recorded.
   */
  static Optional<IdentityEvent> repeated(
      IdentityEventRepository events,
      DirectoryAccount account,
      IdentityEventType type,
      IdentityEventSource source,
      String payload) {
    if (source == IdentityEventSource.ON_DEMAND
        || account.uidmRequestNo() == null
        || account.windowsId() == null) {
      return Optional.empty();
    }
    Set<IdentityEventType> kind =
        type == IdentityEventType.MEMBERSHIP
            ? EnumSet.of(IdentityEventType.MEMBERSHIP)
            : EnumSet.complementOf(EnumSet.of(IdentityEventType.MEMBERSHIP));
    return events
        .findFirstBySourceAndWindowsIdAndEventTypeInOrderByIdDesc(source, account.windowsId(), kind)
        .filter(
            e ->
                e.getEventType() == type
                    && account.uidmRequestNo().equals(e.getUidmRequestNo())
                    && payload.equals(e.getPayload())
                    && (e.getStatus() == IdentityEventStatus.APPLIED
                        || e.getStatus() == IdentityEventStatus.NO_CHANGE));
  }
}
