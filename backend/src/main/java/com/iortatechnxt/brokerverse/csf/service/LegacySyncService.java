package com.iortatechnxt.brokerverse.csf.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.csf.domain.ChangedField;
import com.iortatechnxt.brokerverse.csf.domain.CsfContactChange;
import com.iortatechnxt.brokerverse.csf.domain.CsfContactChangeRepository;
import com.iortatechnxt.brokerverse.csf.domain.CsfSyncOutbox;
import com.iortatechnxt.brokerverse.csf.domain.CsfSyncOutboxRepository;
import com.iortatechnxt.brokerverse.csf.domain.OutboxStatus;
import com.iortatechnxt.brokerverse.csf.domain.SyncStatus;
import com.iortatechnxt.brokerverse.csf.domain.TargetSystem;
import com.iortatechnxt.brokerverse.csf.service.port.ContactSyncGateway;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-back of the applied contact changes to QPS and EBIX while they coexist (FR-CSF-022; CSQ01):
 * every change gets one outbox row per legacy system with its payload, QUEUED when {@code
 * CSF_LEGACY_SYNC_ENABLED} is on, NOT_CONFIGURED otherwise. The job {@code CSF_LEGACY_SYNC} sends
 * the queued and failed rows through the {@link ContactSyncGateway}, first queueing the rows kept
 * while the sync was off (replay); a failed sending raises {@code CSF_SYNC_FAILED}.
 */
@Service
@Transactional
public class LegacySyncService {

  private final CsfSyncOutboxRepository outbox;
  private final CsfContactChangeRepository changes;
  private final ContactSyncGateway gateway;
  private final CsfParameters parameters;
  private final AlertService alerts;
  private final ObjectMapper json;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param outbox outbox rows
   * @param changes contact changes
   * @param gateway legacy transport
   * @param parameters CSF parameters
   * @param alerts alerts
   * @param json JSON writer of the payloads
   * @param clock clock
   */
  public LegacySyncService(
      CsfSyncOutboxRepository outbox,
      CsfContactChangeRepository changes,
      ContactSyncGateway gateway,
      CsfParameters parameters,
      AlertService alerts,
      ObjectMapper json,
      Clock clock) {
    this.outbox = outbox;
    this.changes = changes;
    this.gateway = gateway;
    this.parameters = parameters;
    this.alerts = alerts;
    this.json = json;
    this.clock = clock;
  }

  /**
   * Puts an applied change in the outbox for every legacy system and sets its sync status.
   *
   * @param change applied change
   * @param bankCif bank CIF of the client, may be null
   */
  public void queue(CsfContactChange change, String bankCif) {
    boolean enabled = parameters.legacySyncEnabled();
    OutboxStatus status = enabled ? OutboxStatus.QUEUED : OutboxStatus.NOT_CONFIGURED;
    String payload = payload(change, bankCif);
    for (TargetSystem target : TargetSystem.values()) {
      outbox.save(new CsfSyncOutbox(change.getId(), target, payload, status));
    }
    change.syncState(enabled ? SyncStatus.QUEUED : SyncStatus.NOT_CONFIGURED);
  }

  private String payload(CsfContactChange change, String bankCif) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("changeNo", change.getChangeNo());
    body.put("clientCode", change.getClientCode());
    body.put("bankCif", bankCif);
    body.put("appliedAt", change.getAppliedAt().toString());
    body.put("fields", change.getFields().stream().map(f -> fieldOf(f)).toList());
    try {
      return json.writeValueAsString(body);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Map<String, String> fieldOf(ChangedField f) {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("field", f.getField());
    m.put("oldValue", f.getOldValue());
    m.put("newValue", f.getNewValue());
    return m;
  }

  /**
   * The rows the job sends now: none while the sync is off; otherwise the rows kept while it was
   * off are queued first (replay), then every queued and failed row is returned, oldest first.
   *
   * @return outbox row ids
   */
  public List<Long> rowsToSend() {
    if (!parameters.legacySyncEnabled()) {
      return List.of();
    }
    List<CsfSyncOutbox> kept =
        outbox.findByStatusInOrderByIdAsc(EnumSet.of(OutboxStatus.NOT_CONFIGURED));
    kept.forEach(CsfSyncOutbox::requeue);
    kept.stream()
        .map(CsfSyncOutbox::getChangeId)
        .distinct()
        .forEach(id -> changes.findById(id).ifPresent(c -> c.syncState(SyncStatus.QUEUED)));
    return outbox
        .findByStatusInOrderByIdAsc(EnumSet.of(OutboxStatus.QUEUED, OutboxStatus.FAILED))
        .stream()
        .map(CsfSyncOutbox::getId)
        .toList();
  }

  /**
   * Sends one outbox row; a failure is recorded on the row and raises the alert.
   *
   * @param rowId outbox row
   * @return true when sent
   */
  public boolean send(Long rowId) {
    CsfSyncOutbox row = outbox.findById(rowId).orElseThrow();
    boolean sent;
    try {
      gateway.send(row.getTargetSystem(), row.getPayload());
      row.sent(clock.instant());
      sent = true;
    } catch (RuntimeException e) {
      row.failed(e.getMessage(), clock.instant());
      sent = false;
    }
    CsfContactChange change = changes.findById(row.getChangeId()).orElseThrow();
    change.syncState(stateOf(outbox.findByChangeIdOrderByIdAsc(change.getId())));
    if (!sent) {
      alerts.raise(
          CsfCodes.ALERT_SYNC_FAILED,
          new AlertFacts(
              change.getCompanyId(),
              null,
              CsfCodes.ENTITY_CHANGE,
              change.getChangeNo(),
              "Contact change "
                  + change.getChangeNo()
                  + " of "
                  + change.getClientCode()
                  + " was not sent to "
                  + row.getTargetSystem()
                  + ": "
                  + row.getLastError(),
              null,
              CsfCodes.ALERT_SYNC_FAILED + ":" + row.getId()));
    }
    return sent;
  }

  private static SyncStatus stateOf(List<CsfSyncOutbox> rows) {
    if (rows.stream().allMatch(r -> r.getStatus() == OutboxStatus.SENT)) {
      return SyncStatus.SENT;
    }
    if (rows.stream().anyMatch(r -> r.getStatus() == OutboxStatus.FAILED)) {
      return SyncStatus.FAILED;
    }
    return SyncStatus.QUEUED;
  }

  /**
   * Whether a transport to the legacy systems exists (job description and outcome).
   *
   * @return true when connected
   */
  @Transactional(readOnly = true)
  public boolean connected() {
    return gateway.connected();
  }
}
